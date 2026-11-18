package config

import (
	"health/schemas/config"
	"time"
)

type UserConfigResponse struct {
	ID           uint    `json:"id"`
	ShowCreatine bool    `json:"showCreatine"`
	DailyCardio  float64 `json:"dailyCardio"`
}

func ConvertUserConfigToUserConfigResponse(userConfig *config.UserConfig, locale any) UserConfigResponse {
	return UserConfigResponse{
		ID:           userConfig.ID,
		ShowCreatine: userConfig.ShowCreatine,
		DailyCardio:  userConfig.DailyCardio,
	}
}

type HistoryResponse struct {
	ID        uint    `json:"id"`
	EventDate string  `json:"eventDate"`
	Creatine  bool    `json:"creatine"`
	Cardio    float64 `json:"cardio"`
}

func ConvertHistoryToHistoryResponse(history *config.History) HistoryResponse {
	return HistoryResponse{
		ID:        history.ID,
		EventDate: time.Time(history.EventDate).Format("2006-01-02"),
		Creatine:  history.Creatine,
		Cardio:    history.Cardio,
	}
}
