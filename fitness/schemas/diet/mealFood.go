package diet

import "gorm.io/gorm"

type MealFood struct {
	gorm.Model
	// MealFood keeps the legacy column name and holds the catalog food id.
	MealFood    uint   `json:"food"`
	Food        Food   `json:"-" gorm:"foreignKey:MealFood"`
	Quantity    int    `json:"quantity"`
	Observation string `json:"observation"`
	MealID      uint   `json:"meal_id"`
}
