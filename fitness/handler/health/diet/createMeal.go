package diet

import (
	"errors"
	"net/http"

	"health/helper"
	"health/schemas/diet"
	"health/schemas/link"
	"health/service/access"

	"github.com/gin-gonic/gin"
	"github.com/nicksnyder/go-i18n/v2/i18n"
	"gorm.io/gorm"
)

// CreateMealHandler cria uma refeição do aluno autorizado.
// @Summary Cria uma refeição
// @Tags Dieta
// @Accept json
// @Produce json
// @Param request body CreateMealRequest true "Refeição"
// @Success 200 {object} ResponseDiet
// @Failure 400 {object} helper.ErrorResponse
// @Failure 401 {object} helper.ErrorResponse
// @Failure 403 {object} helper.ErrorResponse
// @Failure 404 {object} helper.ErrorResponse
// @Router /diet/meal [post]
func CreateMealHandler(ctx *gin.Context) {
	request := CreateMealRequest{}
	getI18n, _ := ctx.Get("i18n")
	localizer := getI18n.(*i18n.Localizer)

	caller, exists := helper.GatewayCaller(ctx)
	if !exists {
		helper.SendErrorDefault(ctx, http.StatusUnauthorized, localizer)
		return
	}

	if err := ctx.BindJSON(&request); err != nil {
		logger.ErrF("validation error: %v", err.Error())
		helper.SendError(ctx, http.StatusBadRequest, err.Error())
		return
	}

	if err := request.Validate(); err != nil {
		logger.ErrF("validation error: %v", err.Error())
		helper.SendError(ctx, http.StatusBadRequest, err.Error())
		return
	}

	studentID, denied := access.WriteTarget(caller, helper.RequestedStudent(ctx, request.StudentID), link.DomainNutritionist)
	if denied != nil {
		helper.SendErrorDefault(ctx, denied.Status, localizer)
		return
	}

	var existingMeal diet.Meal
	if err := db.Where("name = ? AND student_id = ?", request.Name, studentID).First(&existingMeal).Error; err == nil {
		logger.Err("Meal already exists")
		message := localizer.MustLocalize(&i18n.LocalizeConfig{
			MessageID: "alreadyExists",
		})
		helper.SendError(ctx, http.StatusBadRequest, message)
		return
	} else if !errors.Is(err, gorm.ErrRecordNotFound) {
		logger.ErrF("Error checking meal existence: %s", err.Error())
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, localizer)
		return
	}

	meal := diet.Meal{
		Name:      request.Name,
		StudentID: studentID,
		AuthorID:  caller.ID,
		Hour:      request.Hour,
	}

	for _, food := range request.Foods {
		meal.Foods = append(meal.Foods, diet.MealFood{
			MealFood:    food.FoodID,
			Quantity:    food.Quantity,
			Observation: food.Observation,
		})
	}

	err := db.Transaction(func(tx *gorm.DB) error {
		if err := tx.Create(&meal).Error; err != nil {
			return err
		}
		return tx.Preload("Foods.Food").First(&meal, meal.ID).Error
	})
	if err != nil {
		logger.ErrF("Error in CreateMealHandler: %s", err.Error())
		helper.SendError(ctx, http.StatusInternalServerError, err.Error())
		return
	}

	helper.SendSuccess(ctx, ConvertMealToResponse(&meal))
}
