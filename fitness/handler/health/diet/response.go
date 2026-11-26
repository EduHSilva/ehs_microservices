package diet

import (
	"time"

	"health/schemas/diet"
)

type ResponseFood struct {
	Message string           `json:"message"`
	Data    ResponseDataFood `json:"data"`
}

type ResponseDataFood struct {
	TagID       uint       `json:"tag_id"`
	FoodID      uint       `json:"food_id"`
	FoodName    string     `json:"food_name"`
	Quantity    int        `json:"quantity"`
	Observation string     `json:"observation"`
	Photo       PhotoField `json:"photo"`
	// Macros por 100 g do catálogo; os clientes escalam pela quantidade.
	Calories float64 `json:"calories"`
	Protein  float64 `json:"protein"`
	Carbs    float64 `json:"carbs"`
	Fat      float64 `json:"fat"`
	Fiber    float64 `json:"fiber"`
	// Medida caseira de referência do catálogo (ex.: fatia = 25 g).
	PortionLabel string  `json:"portion_label"`
	PortionGrams float64 `json:"portion_grams"`
}

type PhotoField struct {
	Thumb string `json:"thumb"`
}

type ResponseData struct {
	ID        uint               `json:"id"`
	CreateAt  time.Time          `json:"createAt"`
	UpdateAt  time.Time          `json:"updateAt"`
	DeletedAt time.Time          `json:"deletedAt,omitempty"`
	Name      string             `json:"name"`
	Hour      string             `json:"hour"`
	StudentID string             `json:"student_id"`
	AuthorID  string             `json:"author_id"`
	Foods     []ResponseDataFood `json:"foods"`
}

type ResponseDiet struct {
	Message string       `json:"message"`
	Data    ResponseData `json:"data"`
}

func ConvertFoodToResponse(food *diet.MealFood) ResponseDataFood {
	// tag_id is kept for older clients; it now carries the catalog food id,
	// the same value as food_id.
	return ResponseDataFood{
		TagID:        food.MealFood,
		FoodID:       food.MealFood,
		FoodName:     food.Food.Name,
		Quantity:     food.Quantity,
		Observation:  food.Observation,
		Calories:     food.Food.Calories,
		Protein:      food.Food.Protein,
		Carbs:        food.Food.Carbs,
		Fat:          food.Food.Fat,
		Fiber:        food.Food.Fiber,
		PortionLabel: food.Food.PortionLabel,
		PortionGrams: food.Food.PortionGrams,
	}
}

type FoodResponse struct {
	ID           uint    `json:"id"`
	Name         string  `json:"name"`
	Calories     float64 `json:"calories"`
	Protein      float64 `json:"protein"`
	Carbs        float64 `json:"carbs"`
	Fat          float64 `json:"fat"`
	Fiber        float64 `json:"fiber"`
	PortionLabel string  `json:"portion_label"`
	PortionGrams float64 `json:"portion_grams"`
}

func ConvertFoodsToResponse(food *diet.Food) FoodResponse {
	return FoodResponse{
		ID:           food.ID,
		Name:         food.Name,
		Calories:     food.Calories,
		Protein:      food.Protein,
		Carbs:        food.Carbs,
		Fat:          food.Fat,
		Fiber:        food.Fiber,
		PortionLabel: food.PortionLabel,
		PortionGrams: food.PortionGrams,
	}
}

func ConvertMealToResponse(meal *diet.Meal) ResponseData {
	foodResponse := make([]ResponseDataFood, len(meal.Foods))

	for i, food := range meal.Foods {
		foodResponse[i] = ConvertFoodToResponse(&food)
	}

	return ResponseData{
		ID:        meal.ID,
		CreateAt:  meal.CreatedAt,
		UpdateAt:  meal.UpdatedAt,
		DeletedAt: meal.DeletedAt.Time,
		Name:      meal.Name,
		Foods:     foodResponse,
		Hour:      meal.Hour,
		StudentID: meal.StudentID,
		AuthorID:  meal.AuthorID,
	}
}
