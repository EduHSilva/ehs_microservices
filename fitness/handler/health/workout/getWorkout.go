package workout

import (
	"errors"
	"net/http"

	"health/helper"
	"health/schemas/workout"
	"health/service/access"

	"github.com/gin-gonic/gin"
	"github.com/nicksnyder/go-i18n/v2/i18n"
	"gorm.io/gorm"
)

// GetWorkoutHandler devolve um treino do aluno autorizado.
// @Summary Detalha um treino
// @Tags Treinos
// @Produce json
// @Param id query int true "ID do treino"
// @Param student_id query string false "Aluno (ausente = o próprio usuário)"
// @Success 200 {object} ResponseWorkout
// @Failure 401 {object} helper.ErrorResponse
// @Failure 403 {object} helper.ErrorResponse
// @Failure 404 {object} helper.ErrorResponse
// @Router /workout [get]
func GetWorkoutHandler(ctx *gin.Context) {
	id := ctx.Query("id")
	getI18n, _ := ctx.Get("i18n")
	locale, _ := ctx.Get("locale")
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

	studentID, denied := access.ReadTarget(caller, helper.RequestedStudent(ctx, ""))
	if denied != nil {
		helper.SendErrorDefault(ctx, denied.Status, localizer)
		return
	}

	work := workout.Workout{}
	err := db.Where("id = ? AND student_id = ?", id, studentID).
		Preload("Exercises").
		Preload("Exercises.Exercise").
		First(&work).Error
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			helper.SendErrorDefault(ctx, http.StatusNotFound, localizer)
			return
		}
		logger.ErrF("error loading workout: %s", err.Error())
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, localizer)
		return
	}

	helper.SendSuccess(ctx, ConvertWorkoutToWorkoutResponse(&work, locale))
}
