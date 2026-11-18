package config

import "health/helper"

type UpdateUserConfigRequest struct {
	ShowCreatine *bool    `json:"showCreatine"`
	DailyCardio  *float64 `json:"dailyCardio"`
	// StudentID targets a linked student; empty means the caller's own config.
	StudentID string `json:"student_id"`
}

func (r UpdateUserConfigRequest) Validate() error {
	if r.ShowCreatine == nil && r.DailyCardio == nil {
		return helper.ErrParamIsRequired("showCreatine or dailyCardio", "request body")
	}
	return nil
}

type UpdateHistoryRequest struct {
	Creatine *bool    `json:"creatine"`
	Cardio   *float64 `json:"cardio"`
}

func (r UpdateHistoryRequest) Validate() error {
	if r.Creatine == nil && r.Cardio == nil {
		return helper.ErrParamIsRequired("creatine or cardio", "request body")
	}
	return nil
}
