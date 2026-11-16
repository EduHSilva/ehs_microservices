package workout

import (
	"net/http"

	"health/helper"
	"health/schemas/workout"
	"health/service/access"

	"github.com/gin-gonic/gin"
	"github.com/nicksnyder/go-i18n/v2/i18n"
)

// GetWorkoutsHandler lista os treinos do aluno autorizado.
// @Summary Lista os treinos
// @Description Lista vazia é devolvida como [] e não como null.
// @Tags Treinos
// @Produce json
// @Param student_id query string false "Aluno (ausente = o próprio usuário)"
// @Success 200 {object} map[string]interface{}
// @Failure 401 {object} helper.ErrorResponse
// @Failure 403 {object} helper.ErrorResponse
// @Failure 404 {object} helper.ErrorResponse
// @Router /workouts [get]
func GetWorkoutsHandler(ctx *gin.Context) {
	workouts := make([]workout.Workout, 0)
	workoutResponses := make([]ResponseData, 0)

	getI18n, _ := ctx.Get("i18n")
	locale, _ := ctx.Get("locale")
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

	if err := db.Where("student_id = ?", studentID).Order("name").
		Preload("Exercises").
		Preload("Exercises.Exercise").
		Find(&workouts).Error; err != nil {
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, localizer)
		return
	}

	for _, work := range workouts {
		workoutResponse := ConvertWorkoutToWorkoutResponse(&work, locale)
		workoutResponses = append(workoutResponses, workoutResponse)
	}

	helper.SendSuccess(ctx, workoutResponses)
}
