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

// UpdateMealHandler atualiza uma refeição do aluno autorizado.
// @Summary Atualiza uma refeição
// @Tags Dieta
// @Accept json
// @Produce json
// @Param id query int true "ID da refeição"
// @Param request body UpdateMealRequest true "Refeição"
// @Success 200 {object} ResponseDiet
// @Failure 400 {object} helper.ErrorResponse
// @Failure 401 {object} helper.ErrorResponse
// @Failure 403 {object} helper.ErrorResponse
// @Failure 404 {object} helper.ErrorResponse
// @Router /diet/meal [put]
func UpdateMealHandler(ctx *gin.Context) {
	request := UpdateMealRequest{}

	getI18n, _ := ctx.Get("i18n")
	localizer := getI18n.(*i18n.Localizer)

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

	id := ctx.Query("id")
	if id == "" {
		helper.SendError(ctx, http.StatusBadRequest,
			helper.ErrParamIsRequired("id", "query param").Error())
		return
	}
	caller, exists := helper.GatewayCaller(ctx)
	if !exists {
		helper.SendErrorDefault(ctx, http.StatusUnauthorized, localizer)
		return
	}

	studentID, denied := access.WriteTarget(caller, helper.RequestedStudent(ctx, request.StudentID), link.DomainNutritionist)
	if denied != nil {
		helper.SendErrorDefault(ctx, denied.Status, localizer)
		return
	}

	meal := &diet.Meal{}
	err := db.Where("id = ? AND student_id = ?", id, studentID).First(meal).Error
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			helper.SendErrorDefault(ctx, http.StatusNotFound, localizer)
			return
		}
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, localizer)
		return
	}

	err = db.Transaction(func(tx *gorm.DB) error {
		meal.Name = request.Name
		meal.Hour = request.Hour
		meal.AuthorID = caller.ID
		if err := tx.Save(meal).Error; err != nil {
			return err
		}

		if err := tx.Where("meal_id = ?", meal.ID).Delete(&diet.MealFood{}).Error; err != nil {
			return err
		}
		for _, food := range request.Foods {
			newFood := diet.MealFood{
				MealFood:    food.FoodID,
				Quantity:    food.Quantity,
				Observation: food.Observation,
				MealID:      meal.ID,
			}
			if err := tx.Create(&newFood).Error; err != nil {
				return err
			}
		}

		meal.Foods = nil
		return tx.Preload("Foods.Food").First(meal, meal.ID).Error
	})
	if err != nil {
		logger.ErrF("error updating: %s", err.Error())
		helper.SendError(ctx, http.StatusInternalServerError, err.Error())
		return
	}

	helper.SendSuccess(ctx, ConvertMealToResponse(meal))
}
