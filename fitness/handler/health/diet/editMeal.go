package diet

import (
	"health/helper"
	"health/schemas/diet"
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/nicksnyder/go-i18n/v2/i18n"
)

func UpdateMealHandler(ctx *gin.Context) {
	request := UpdateMealRequest{}

	getI18n, _ := ctx.Get("i18n")

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
	userID, exists := helper.GatewayUserID(ctx)
	if !exists {
		helper.SendErrorDefault(ctx, http.StatusUnauthorized, getI18n.(*i18n.Localizer))
		return
	}

	meal := &diet.Meal{}
	if err := db.Where("id = ? AND user_id = ?", id, userID).Preload("Foods").First(meal).Error; err != nil {
		helper.SendErrorDefault(ctx, http.StatusNotFound, getI18n.(*i18n.Localizer))
		return
	}
	if request.Name != "" {
		meal.Name = request.Name
	}

	if request.Hour != "" {
		meal.Hour = request.Hour
	}

	if len(request.Foods) != 0 {
		if err := db.Where("meal_id = ?", meal.ID).Delete(&diet.Food{}).Error; err != nil {
			logger.ErrF("error on find meal: %s", err.Error())
			helper.SendError(ctx, http.StatusInternalServerError, "Failed to delete associated foods")
			return
		}

		for _, food := range request.Foods {
			newFood := diet.MealFood{
				MealFood:    food.FoodID,
				Quantity:    food.Quantity,
				Observation: food.Observation,
				MealID:      meal.ID,
			}
			if err := db.Create(&newFood).Error; err != nil {
				logger.ErrF("error updating foods: %s", err.Error())
				helper.SendError(ctx, http.StatusInternalServerError, err.Error())
				return
			}
		}
	}

	if err := db.Save(&meal).Error; err != nil {
		logger.ErrF("error updating: %s", err.Error())
		helper.SendError(ctx, http.StatusInternalServerError, err.Error())
		return
	}

	helper.SendSuccess(ctx, ConvertMealToResponse(meal))
}
