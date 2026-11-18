package workout

import (
	"errors"
	"net/http"

	"health/helper"
	"health/schemas/link"
	"health/schemas/workout"
	"health/service/access"

	"github.com/gin-gonic/gin"
	"github.com/nicksnyder/go-i18n/v2/i18n"
	"gorm.io/gorm"
)

// UpdateWorkoutLoadHandler ajusta a carga de um exercício do treino durante a execução.
// @Summary Atualiza a carga de um exercício do treino
// @Description Alteração parcial: mantém nome, ordem e os demais exercícios do treino.
// @Tags Treinos
// @Accept json
// @Produce json
// @Param request body UpdateWorkoutLoadRequest true "Carga"
// @Success 200 {object} ResponseWorkout
// @Failure 400 {object} helper.ErrorResponse
// @Failure 401 {object} helper.ErrorResponse
// @Failure 403 {object} helper.ErrorResponse
// @Failure 404 {object} helper.ErrorResponse
// @Router /workout/load [put]
func UpdateWorkoutLoadHandler(ctx *gin.Context) {
	request := UpdateWorkoutLoadRequest{}
	getI18n, _ := ctx.Get("i18n")
	locale, _ := ctx.Get("locale")
	localizer := getI18n.(*i18n.Localizer)

	if err := ctx.ShouldBindJSON(&request); err != nil {
		helper.SendError(ctx, http.StatusBadRequest, err.Error())
		return
	}
	if err := request.Validate(); err != nil {
		helper.SendError(ctx, http.StatusBadRequest, err.Error())
		return
	}

	caller, exists := helper.GatewayCaller(ctx)
	if !exists {
		helper.SendErrorDefault(ctx, http.StatusUnauthorized, localizer)
		return
	}

	studentID, denied := access.WriteTarget(caller, helper.RequestedStudent(ctx, request.StudentID), link.DomainPersonal)
	if denied != nil {
		helper.SendErrorDefault(ctx, denied.Status, localizer)
		return
	}

	work := &workout.Workout{}
	if err := db.Where("id = ? AND student_id = ?", request.WorkoutID, studentID).First(work).Error; err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			helper.SendErrorDefault(ctx, http.StatusNotFound, localizer)
			return
		}
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, localizer)
		return
	}

	err := db.Transaction(func(tx *gorm.DB) error {
		result := tx.Model(&workout.ExerciseWorkout{}).
			Where("workout_id = ? AND exercise_id = ?", work.ID, request.ExerciseID).
			Update("load", request.Load)
		if result.Error != nil {
			return result.Error
		}
		if result.RowsAffected == 0 {
			return gorm.ErrRecordNotFound
		}
		work.AuthorID = caller.ID
		if err := tx.Save(work).Error; err != nil {
			return err
		}
		return preloadWorkoutExercises(tx).First(work, work.ID).Error
	})
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			helper.SendErrorDefault(ctx, http.StatusNotFound, localizer)
			return
		}
		logger.ErrF("error updating load: %s", err.Error())
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, localizer)
		return
	}

	helper.SendSuccess(ctx, ConvertWorkoutToWorkoutResponse(work, locale))
}
