package workout

import "gorm.io/gorm"

type ExerciseWorkout struct {
	gorm.Model
	WorkoutID   uint     `json:"workout_id"`
	ExerciseID  uint     `json:"exercise_id"`
	Exercise    Exercise `json:"exercise"`
	Series      int      `json:"series"`
	Load        float64  `json:"load"`
	RestSeconds int      `json:"rest_seconds"`
	Repetitions int      `json:"repetitions"`
	// DurationSeconds marks a time-based exercise (cardio); 0 means sets/reps.
	DurationSeconds uint   `json:"duration_seconds"`
	Notes           string `json:"notes"`
}
