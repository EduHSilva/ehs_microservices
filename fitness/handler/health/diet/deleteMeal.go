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

// DeleteMealHandler exclui uma refeição do aluno autorizado.
// @Summary Exclui uma refeição
// @Tags Dieta
// @Produce json
// @Param id query int true "ID da refeição"
// @Param student_id query string false "Aluno (ausente = o próprio usuário)"
// @Success 200 {object} ResponseDiet
// @Failure 401 {object} helper.ErrorResponse
// @Failure 403 {object} helper.ErrorResponse
// @Failure 404 {object} helper.ErrorResponse
// @Router /diet/meal [delete]
func DeleteMealHandler(ctx *gin.Context) {
	id := ctx.Query("id")

	getI18n, _ := ctx.Get("i18n")
	localizer := getI18n.(*i18n.Localizer)

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

	studentID, denied := access.WriteTarget(caller, helper.RequestedStudent(ctx, ""), link.DomainNutritionist)
	if denied != nil {
		helper.SendErrorDefault(ctx, denied.Status, localizer)
		return
	}

	meal := &diet.Meal{}
	err := db.Where("id = ? AND student_id = ?", id, studentID).
		Preload("Foods.Food").
		First(meal).Error
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			helper.SendErrorDefault(ctx, http.StatusNotFound, localizer)
			return
		}
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, localizer)
		return
	}

	if err := db.Delete(&meal).Error; err != nil {
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, localizer)
		return
	}

	helper.SendSuccess(ctx, ConvertMealToResponse(meal))
}
