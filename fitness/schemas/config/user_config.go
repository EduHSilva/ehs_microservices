package config

import "gorm.io/gorm"

type UserConfig struct {
	gorm.Model
	UserID       string  `gorm:"type:uuid;not null"`
	ShowCreatine bool    `gorm:"type:bool" json:"showCreatine"`
	DailyCardio  float64 `json:"dailyCardio"`
	ShowWorkout  bool    `gorm:"type:bool" json:"showWorkout"`
}
