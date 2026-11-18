package config

import "health/helper"

type UpdateUserConfigRequest struct {
	ShowCreatine *bool    `json:"showCreatine"`
	DailyCardio  *float64 `json:"dailyCardio"`
	ShowWorkout  *bool    `json:"showWorkout"`
	// StudentID targets a linked student; empty means the caller's own config.
	StudentID string `json:"student_id"`
}

func (r UpdateUserConfigRequest) Validate() error {
	if r.ShowCreatine == nil && r.DailyCardio == nil && r.ShowWorkout == nil {
		return helper.ErrParamIsRequired("showCreatine, dailyCardio or showWorkout", "request body")
	}
	return nil
}

type UpdateHistoryRequest struct {
	Creatine *bool    `json:"creatine"`
	Cardio   *float64 `json:"cardio"`
	Trained  *bool    `json:"trained"`
}

func (r UpdateHistoryRequest) Validate() error {
	if r.Creatine == nil && r.Cardio == nil && r.Trained == nil {
		return helper.ErrParamIsRequired("creatine, cardio or trained", "request body")
	}
	return nil
}
