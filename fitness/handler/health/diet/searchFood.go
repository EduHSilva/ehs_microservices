package diet

import (
	"net/http"
	"strings"

	"health/helper"
	"health/schemas/diet"

	"github.com/gin-gonic/gin"
	"github.com/nicksnyder/go-i18n/v2/i18n"
)

// SearchFoodHandler busca no catálogo global de alimentos.
// @Summary Busca alimentos
// @Tags Dieta
// @Produce json
// @Param query query string false "Trecho do nome"
// @Success 200 {object} map[string]interface{}
// @Failure 401 {object} helper.ErrorResponse
// @Router /diet/meal/food [get]
func SearchFoodHandler(ctx *gin.Context) {
	foods := make([]diet.Food, 0)
	foodsResponses := make([]FoodResponse, 0)

	getI18n, _ := ctx.Get("i18n")
	localizer := getI18n.(*i18n.Localizer)

	if _, exists := helper.GatewayCaller(ctx); !exists {
		helper.SendErrorDefault(ctx, http.StatusUnauthorized, localizer)
		return
	}

	query := strings.TrimSpace(ctx.Query("query"))

	if err := db.
		Where("LOWER(name) LIKE ?", "%"+strings.ToLower(query)+"%").
		Order("name ASC").
		Limit(30).
		Find(&foods).Error; err != nil {
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, localizer)
		return
	}

	for _, food := range foods {
		response := ConvertFoodsToResponse(&food)
		foodsResponses = append(foodsResponses, response)
	}

	helper.SendSuccess(ctx, foodsResponses)
}
