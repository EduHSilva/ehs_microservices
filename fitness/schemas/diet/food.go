package diet

import "gorm.io/gorm"

type Food struct {
	gorm.Model

	Code string `gorm:"size:20;uniqueIndex"`
	Name string `gorm:"size:255;not null;index"`

	// Medida caseira de referência (ex.: fatia = 25 g). A quantidade da refeição
	// continua em gramas; a porção serve só para facilitar o preenchimento.
	PortionLabel string  `gorm:"size:40"`
	PortionGrams float64 `gorm:"type:decimal(8,2)"`

	// Macronutrientes (100g)
	Calories float64 `gorm:"type:decimal(8,2)"`
	Protein  float64 `gorm:"type:decimal(8,2)"`
	Carbs    float64 `gorm:"type:decimal(8,2)"`
	Fat      float64 `gorm:"type:decimal(8,2)"`
	Fiber    float64 `gorm:"type:decimal(8,2)"`

	// Outros
	Water       float64 `gorm:"type:decimal(8,2)"`
	Sodium      float64 `gorm:"type:decimal(8,2)"`
	Cholesterol float64 `gorm:"type:decimal(8,2)"`

	// Gorduras
	SaturatedFat float64 `gorm:"type:decimal(8,2)"`
	MonounsatFat float64 `gorm:"type:decimal(8,2)"`
	PolyunsatFat float64 `gorm:"type:decimal(8,2)"`
	TransFat     float64 `gorm:"type:decimal(8,2)"`

	// Minerais
	Calcium   float64 `gorm:"type:decimal(8,2)"`
	Iron      float64 `gorm:"type:decimal(8,2)"`
	Magnesium float64 `gorm:"type:decimal(8,2)"`
	Phosphor  float64 `gorm:"type:decimal(8,2)"`
	Potassium float64 `gorm:"type:decimal(8,2)"`
	Zinc      float64 `gorm:"type:decimal(8,2)"`

	// Vitaminas
	VitaminA float64 `gorm:"type:decimal(8,2)"`
	VitaminC float64 `gorm:"type:decimal(8,2)"`
	VitaminD float64 `gorm:"type:decimal(8,2)"`
	VitaminE float64 `gorm:"type:decimal(8,2)"`
	B1       float64 `gorm:"type:decimal(8,2)"`
	B2       float64 `gorm:"type:decimal(8,2)"`
	B6       float64 `gorm:"type:decimal(8,2)"`
	B12      float64 `gorm:"type:decimal(8,2)"`

	AvailableCarbs float64
	Ash            float64
	Alcohol        float64
	AddedSugar     float64
	AddedSalt      float64
	Selenium       float64
	Copper         float64
	Niacin         float64
	Folate         float64
	EnergyKJ       float64
}
