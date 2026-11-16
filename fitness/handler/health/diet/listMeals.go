package diet

import (
	"net/http"

	"health/helper"
	"health/schemas/diet"
	"health/service/access"

	"github.com/gin-gonic/gin"
	"github.com/nicksnyder/go-i18n/v2/i18n"
)

// GetMealsHandler lista as refeições do aluno autorizado.
// @Summary Lista as refeições
// @Description Lista vazia é devolvida como [] e não como null.
// @Tags Dieta
// @Produce json
// @Param student_id query string false "Aluno (ausente = o próprio usuário)"
// @Success 200 {object} map[string]interface{}
// @Failure 401 {object} helper.ErrorResponse
// @Failure 403 {object} helper.ErrorResponse
// @Failure 404 {object} helper.ErrorResponse
// @Router /diet/meals [get]
func GetMealsHandler(ctx *gin.Context) {
	meals := make([]diet.Meal, 0)
	mealResponses := make([]ResponseData, 0)

	getI18n, _ := ctx.Get("i18n")
	localizer := getI18n.(*i18n.Localizer)

	caller, exists := helper.GatewayCaller(ctx)
	if !exists {
		helper.SendErrorDefault(ctx, http.StatusUnauthorized, localizer)
		return
	}

	studentID, denied := access.ReadTarget(caller, helper.RequestedStudent(ctx, ""))
	if denied != nil {
		helper.SendErrorDefault(ctx, denied.Status, localizer)
		return
	}

	if err := db.Where("student_id = ?", studentID).Order("hour").
		Preload("Foods.Food").
		Find(&meals).Error; err != nil {
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, localizer)
		return
	}

	for _, meal := range meals {
		mealResponse := ConvertMealToResponse(&meal)
		mealResponses = append(mealResponses, mealResponse)
	}

	helper.SendSuccess(ctx, mealResponses)
}
