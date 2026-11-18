package workout

import (
	"health/config"

	"gorm.io/gorm"
)

var (
	logger *config.Logger
	db     *gorm.DB
)

func InitHandler() {
	logger = config.GetLogger("handler workout")
	db = config.GetDB()
}

// preloadWorkoutExercises loads exercises in the order defined by the user.
// Position 0 is legacy data, so id breaks ties for rows created before the column.
func preloadWorkoutExercises(tx *gorm.DB) *gorm.DB {
	return tx.
		Preload("Exercises", func(query *gorm.DB) *gorm.DB {
			return query.Order("position ASC, id ASC")
		}).
		Preload("Exercises.Exercise")
}
