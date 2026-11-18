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

// CreateWorkoutHandler cria um treino para o aluno autorizado.
// @Summary Cria um treino
// @Description Quando student_id é enviado, o profissional precisa ter vínculo ativo no domínio personal.
// @Tags Treinos
// @Accept json
// @Produce json
// @Param request body CreateWorkoutRequest true "Treino"
// @Success 200 {object} ResponseWorkout
// @Failure 400 {object} helper.ErrorResponse
// @Failure 401 {object} helper.ErrorResponse
// @Failure 403 {object} helper.ErrorResponse
// @Failure 404 {object} helper.ErrorResponse
// @Router /workout [post]
func CreateWorkoutHandler(ctx *gin.Context) {
	request := CreateWorkoutRequest{}
	getI18n, _ := ctx.Get("i18n")
	locale, _ := ctx.Get("locale")
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

	studentID, denied := access.WriteTarget(caller, helper.RequestedStudent(ctx, request.StudentID), link.DomainPersonal)
	if denied != nil {
		helper.SendErrorDefault(ctx, denied.Status, localizer)
		return
	}

	var existingWorkout workout.Workout
	if err := db.Where("name = ? AND student_id = ?", request.Name, studentID).First(&existingWorkout).Error; err == nil {
		logger.Err("Workout already exists")
		message := localizer.MustLocalize(&i18n.LocalizeConfig{
			MessageID: "alreadyExists",
		})
		helper.SendError(ctx, http.StatusBadRequest, message)
		return
	} else if !errors.Is(err, gorm.ErrRecordNotFound) {
		logger.ErrF("Error checking workout existence: %s", err.Error())
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, localizer)
		return
	}

	work := workout.Workout{
		Name:      request.Name,
		StudentID: studentID,
		AuthorID:  caller.ID,
	}

	for _, e := range request.Exercises {
		work.Exercises = append(work.Exercises, workout.ExerciseWorkout{
			ExerciseID:      e.ExerciseID,
			WorkoutID:       e.WorkoutID,
			Series:          e.Series,
			Load:            e.Load,
			RestSeconds:     e.RestSeconds,
			Repetitions:     e.Repetitions,
			DurationSeconds: e.DurationSeconds,
			Notes:           e.Notes,
		})
	}

	err := db.Transaction(func(tx *gorm.DB) error {
		if err := tx.Create(&work).Error; err != nil {
			return err
		}
		return tx.Preload("Exercises").Preload("Exercises.Exercise").First(&work, work.ID).Error
	})
	if err != nil {
		logger.ErrF("Error in CreateWorkoutHandler: %s", err.Error())
		helper.SendError(ctx, http.StatusInternalServerError, err.Error())
		return
	}

	helper.SendSuccess(ctx, ConvertWorkoutToWorkoutResponse(&work, locale))
}
