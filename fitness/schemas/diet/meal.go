package diet

import "gorm.io/gorm"

type Meal struct {
	gorm.Model
	Name      string     `json:"name"`
	Hour      string     `json:"hour"`
	Foods     []MealFood `json:"foods" gorm:"foreignKey:MealID"`
	StudentID string     `json:"student_id" gorm:"type:uuid;not null;index"`
	AuthorID  string     `json:"author_id" gorm:"type:uuid;not null"`
}
