package workout

import (
	"health/helper"
)

type CreateExerciseRequest struct {
	Name     string `json:"name"`
	BodyPart string `json:"body_part"`
}

type CreateWorkoutRequest struct {
	Name      string                   `json:"name"`
	StudentID string                   `json:"student_id"`
	Exercises []ExerciseWorkoutRequest `json:"exercises"`
}

type ExerciseWorkoutRequest struct {
	WorkoutID   uint    `json:"workout_id"`
	ExerciseID  uint    `json:"exercise_id"`
	Series      int     `json:"series"`
	Load        float64 `json:"load"`
	RestSeconds int     `json:"rest_seconds"`
	Repetitions int     `json:"repetitions"`
	// DurationSeconds is used by time-based (cardio) exercises instead of sets/reps.
	DurationSeconds uint   `json:"duration_seconds"`
	Notes           string `json:"notes"`
}

func (r CreateWorkoutRequest) Validate() error {

	if r.Name == "" {
		return helper.ErrParamIsRequired("name", "string")
	}
	if len(r.Exercises) == 0 {
		return helper.ErrParamIsRequired("exercises", "exercise")
	}

	return nil
}

// UpdateWorkoutLoadRequest changes only the prescribed load of one exercise in
// a workout; it is used by the app while the student executes the workout.
type UpdateWorkoutLoadRequest struct {
	WorkoutID  uint    `json:"workout_id"`
	ExerciseID uint    `json:"exercise_id"`
	Load       float64 `json:"load"`
	// StudentID targets a linked student; empty means the caller's own plan.
	StudentID string `json:"student_id"`
}

func (r UpdateWorkoutLoadRequest) Validate() error {
	if r.WorkoutID == 0 {
		return helper.ErrParamIsRequired("workout_id", "integer")
	}
	if r.ExerciseID == 0 {
		return helper.ErrParamIsRequired("exercise_id", "integer")
	}
	if r.Load < 0 || r.Load > 1000 {
		return helper.ErrParamIsRequired("load", "value between 0 and 1000")
	}
	return nil
}

type UpdateWorkoutRequest struct {
	Name      string                   `json:"name"`
	StudentID string                   `json:"student_id"`
	Exercises []ExerciseWorkoutRequest `json:"exercises"`
	// DayOfWeek follows ISO-8601 (1 = Monday, 7 = Sunday). Absent keeps the
	// current day; 0 removes the workout from the schedule.
	DayOfWeek *uint `json:"dayOfWeek"`
}

// UpdateWorkoutWeekRequest replaces the student's weekly workout schedule.
// DayOfWeek follows ISO-8601: 1 is Monday and 7 is Sunday. A null workoutId
// leaves that day without a workout.
type UpdateWorkoutWeekRequest struct {
	StudentID string                  `json:"student_id"`
	Days      []WorkoutWeekDayRequest `json:"days"`
}

type WorkoutWeekDayRequest struct {
	DayOfWeek uint  `json:"dayOfWeek"`
	WorkoutID *uint `json:"workoutId"`
}

func (r UpdateWorkoutWeekRequest) Validate() error {
	if len(r.Days) != 7 {
		return helper.ErrParamIsRequired("days", "seven days of the week")
	}

	days := make(map[uint]bool, len(r.Days))
	workouts := make(map[uint]bool)
	for _, day := range r.Days {
		if day.DayOfWeek < 1 || day.DayOfWeek > 7 || days[day.DayOfWeek] {
			return helper.ErrParamIsRequired("days.dayOfWeek", "unique value between 1 and 7")
		}
		days[day.DayOfWeek] = true

		if day.WorkoutID != nil {
			if *day.WorkoutID == 0 || workouts[*day.WorkoutID] {
				return helper.ErrParamIsRequired("days.workoutId", "unique workout id")
			}
			workouts[*day.WorkoutID] = true
		}
	}

	return nil
}

func (r UpdateWorkoutRequest) Validate() error {
	if r.Name == "" {
		return helper.ErrParamIsRequired("name", "string")
	}
	if len(r.Exercises) == 0 {
		return helper.ErrParamIsRequired("exercises", "exercise")
	}
	if r.DayOfWeek != nil && *r.DayOfWeek > 7 {
		return helper.ErrParamIsRequired("dayOfWeek", "value between 0 and 7")
	}
	return nil
}
