package workout

import (
	"gorm.io/gorm"
)

type Workout struct {
	gorm.Model
	Name      string            `json:"name"`
	Exercises []ExerciseWorkout `json:"exercises" gorm:"foreignKey:WorkoutID"`
	StudentID string            `gorm:"type:uuid;not null;uniqueIndex:idx_workouts_student_day,where:day_of_week > 0 AND deleted_at IS NULL" json:"student_id"`
	AuthorID  string            `gorm:"type:uuid;not null" json:"author_id"`
	DayOfWeek uint              `gorm:"check:day_of_week BETWEEN 0 AND 7;uniqueIndex:idx_workouts_student_day,where:day_of_week > 0 AND deleted_at IS NULL" json:"dayOfWeek"`
}
