package config

import (
	_ "embed"

	"gorm.io/gorm"
)

//go:embed lgpd_purge.sql
var lgpdPurgeSQL string

func installLgpdPurge(db *gorm.DB) error {
	return db.Exec(lgpdPurgeSQL).Error
}
