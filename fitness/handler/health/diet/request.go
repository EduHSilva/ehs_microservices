package diet

import (
	"health/helper"
)

type CreateMealRequest struct {
	Name      string        `json:"name"`
	Hour      string        `json:"hour"`
	StudentID string        `json:"student_id"`
	Foods     []FoodRequest `json:"foods"`
}

type FoodRequest struct {
	FoodID      uint   `json:"food_id"`
	Quantity    int    `json:"quantity"`
	Observation string `json:"observation"`
}

func (r CreateMealRequest) Validate() error {

	if r.Name == "" {
		return helper.ErrParamIsRequired("name", "string")
	}
	if r.Hour == "" {
		return helper.ErrParamIsRequired("hour", "string")
	}
	if len(r.Foods) == 0 {
		return helper.ErrParamIsRequired("foods", "food")
	}

	return nil
}

type UpdateMealRequest struct {
	Name      string        `json:"name"`
	Hour      string        `json:"hour"`
	StudentID string        `json:"student_id"`
	Foods     []FoodRequest `json:"foods"`
}

func (r UpdateMealRequest) Validate() error {
	if r.Name == "" {
		return helper.ErrParamIsRequired("name", "string")
	}
	if r.Hour == "" {
		return helper.ErrParamIsRequired("hour", "string")
	}
	if len(r.Foods) == 0 {
		return helper.ErrParamIsRequired("foods", "food")
	}
	return nil
}
