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

var errWorkoutNotFound = errors.New("workout not found")

type ResponseWeekDay struct {
	DayOfWeek uint   `json:"dayOfWeek"`
	WorkoutID *uint  `json:"workoutId"`
	Name      string `json:"name,omitempty"`
}

var weekDays = [7]uint{1, 2, 3, 4, 5, 6, 7}

func loadWeek(studentID string) ([]ResponseWeekDay, error) {
	var workouts []workout.Workout
	if err := db.Select("id", "name", "day_of_week").
		Where("student_id = ? AND day_of_week > 0", studentID).
		Find(&workouts).Error; err != nil {
		return nil, err
	}

	byDay := make(map[uint]workout.Workout, len(workouts))
	for _, work := range workouts {
		byDay[work.DayOfWeek] = work
	}

	week := make([]ResponseWeekDay, 0, len(weekDays))
	for _, day := range weekDays {
		entry := ResponseWeekDay{DayOfWeek: day}
		if work, ok := byDay[day]; ok {
			id := work.ID
			entry.WorkoutID = &id
			entry.Name = work.Name
		}
		week = append(week, entry)
	}

	return week, nil
}

// GetWorkoutWeekHandler returns the seven days of the student's schedule, with
// a null workoutId for rest days.
// GetWorkoutWeekHandler devolve a agenda da semana (7 dias).
// @Summary Lê a agenda semanal
// @Description Sempre devolve os 7 dias; workoutId nulo significa descanso.
// @Tags Agenda
// @Produce json
// @Param student_id query string false "Aluno (ausente = o próprio usuário)"
// @Success 200 {object} map[string]interface{}
// @Failure 401 {object} helper.ErrorResponse
// @Failure 403 {object} helper.ErrorResponse
// @Failure 404 {object} helper.ErrorResponse
// @Router /workouts/week [get]
func GetWorkoutWeekHandler(ctx *gin.Context) {
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

	week, err := loadWeek(studentID)
	if err != nil {
		logger.ErrF("error loading workout week: %s", err.Error())
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, localizer)
		return
	}

	helper.SendSuccess(ctx, week)
}

// UpdateWorkoutWeekHandler atomically replaces every day in the student's
// workout schedule. Each workout may be assigned to only one day.
// UpdateWorkoutWeekHandler substitui a agenda semanal do aluno autorizado.
// @Summary Grava a agenda semanal
// @Tags Agenda
// @Accept json
// @Produce json
// @Param request body UpdateWorkoutWeekRequest true "Agenda"
// @Success 200 {object} map[string]interface{}
// @Failure 400 {object} helper.ErrorResponse
// @Failure 401 {object} helper.ErrorResponse
// @Failure 403 {object} helper.ErrorResponse
// @Failure 404 {object} helper.ErrorResponse
// @Router /workouts/week [put]
func UpdateWorkoutWeekHandler(ctx *gin.Context) {
	request := UpdateWorkoutWeekRequest{}
	if err := ctx.ShouldBindJSON(&request); err != nil {
		helper.SendError(ctx, http.StatusBadRequest, err.Error())
		return
	}
	if err := request.Validate(); err != nil {
		helper.SendError(ctx, http.StatusBadRequest, err.Error())
		return
	}

	getI18n, _ := ctx.Get("i18n")
	localizer := getI18n.(*i18n.Localizer)
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

	err := db.Transaction(func(tx *gorm.DB) error {
		workoutIDs := make(map[uint]bool)
		var workouts []workout.Workout
		if err := tx.Select("id").Where("student_id = ?", studentID).Find(&workouts).Error; err != nil {
			return err
		}
		for _, work := range workouts {
			workoutIDs[work.ID] = true
		}

		for _, day := range request.Days {
			if day.WorkoutID != nil && !workoutIDs[*day.WorkoutID] {
				return errWorkoutNotFound
			}
		}

		// Clearing first makes the reassignment safe when two workouts swap days.
		if err := tx.Model(&workout.Workout{}).Where("student_id = ?", studentID).Update("day_of_week", 0).Error; err != nil {
			return err
		}
		for _, day := range request.Days {
			if day.WorkoutID == nil {
				continue
			}
			if err := tx.Model(&workout.Workout{}).Where("id = ? AND student_id = ?", *day.WorkoutID, studentID).Update("day_of_week", day.DayOfWeek).Error; err != nil {
				return err
			}
		}
		return nil
	})
	if err != nil {
		if errors.Is(err, errWorkoutNotFound) {
			helper.SendErrorDefault(ctx, http.StatusNotFound, localizer)
			return
		}
		logger.ErrF("error updating workout week: %s", err.Error())
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, localizer)
		return
	}

	week, err := loadWeek(studentID)
	if err != nil {
		logger.ErrF("error loading workout week: %s", err.Error())
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, localizer)
		return
	}

	helper.SendSuccess(ctx, week)
}
