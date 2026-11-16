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

// UpdateWorkoutHandler atualiza nome, exercícios e, opcionalmente, o dia do treino.
// @Summary Atualiza um treino
// @Tags Treinos
// @Accept json
// @Produce json
// @Param id query int true "ID do treino"
// @Param request body UpdateWorkoutRequest true "Treino"
// @Success 200 {object} ResponseWorkout
// @Failure 400 {object} helper.ErrorResponse
// @Failure 401 {object} helper.ErrorResponse
// @Failure 403 {object} helper.ErrorResponse
// @Failure 404 {object} helper.ErrorResponse
// @Router /workout [put]
func UpdateWorkoutHandler(ctx *gin.Context) {
	request := UpdateWorkoutRequest{}

	getI18n, _ := ctx.Get("i18n")
	locale, _ := ctx.Get("locale")
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

	studentID, denied := access.WriteTarget(caller, helper.RequestedStudent(ctx, request.StudentID), link.DomainPersonal)
	if denied != nil {
		helper.SendErrorDefault(ctx, denied.Status, localizer)
		return
	}

	work := &workout.Workout{}
	err := db.Where("id = ? AND student_id = ?", id, studentID).First(work).Error
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			helper.SendErrorDefault(ctx, http.StatusNotFound, localizer)
			return
		}
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, localizer)
		return
	}

	if request.DayOfWeek != nil && *request.DayOfWeek > 0 {
		var taken int64
		db.Model(&workout.Workout{}).
			Where("student_id = ? AND day_of_week = ? AND id <> ?", studentID, *request.DayOfWeek, work.ID).
			Count(&taken)
		if taken > 0 {
			helper.SendError(ctx, http.StatusBadRequest, "dayOfWeek already has a workout")
			return
		}
	}

	err = db.Transaction(func(tx *gorm.DB) error {
		work.Name = request.Name
		work.AuthorID = caller.ID
		if request.DayOfWeek != nil {
			work.DayOfWeek = *request.DayOfWeek
		}
		if err := tx.Save(work).Error; err != nil {
			return err
		}

		if err := tx.Where("workout_id = ?", work.ID).Delete(&workout.ExerciseWorkout{}).Error; err != nil {
			return err
		}
		for _, ex := range request.Exercises {
			newExercise := workout.ExerciseWorkout{
				WorkoutID:   work.ID,
				ExerciseID:  ex.ExerciseID,
				Series:      ex.Series,
				Load:        ex.Load,
				RestSeconds: ex.RestSeconds,
				Repetitions: ex.Repetitions,
				Notes:       ex.Notes,
			}
			if err := tx.Create(&newExercise).Error; err != nil {
				return err
			}
		}

		return tx.Preload("Exercises").Preload("Exercises.Exercise").First(work, work.ID).Error
	})
	if err != nil {
		logger.ErrF("error updating: %s", err.Error())
		helper.SendError(ctx, http.StatusInternalServerError, err.Error())
		return
	}

	helper.SendSuccess(ctx, ConvertWorkoutToWorkoutResponse(work, locale))
}
