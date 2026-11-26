package seeds

import (
	"embed"
	"health/schemas/workout"

	"gorm.io/gorm"
)

//go:embed data/*.json data/*.xlsx data/*.csv
var Files embed.FS

func Load(db *gorm.DB) {
	var count int64

	db.Model(&workout.Exercise{}).Count(&count)
	if count == 0 {
		loadExerciseFromFile(db)
	}

	// O catálogo de alimentos é sempre sincronizado por code (upsert), para que
	// versões novas da TACO e do complemento entrem no boot sem truncate.
	foods := loadFoodFromExcel()
	foods = append(foods, loadComplementaryFoods()...)
	applyFoodPortions(foods, loadFoodPortions())
	seedFoods(db, foods)
}
