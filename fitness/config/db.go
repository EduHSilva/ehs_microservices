package config

import (
	"fmt"
	"health/schemas/config"
	"health/schemas/diet"
	"health/schemas/link"
	"health/schemas/workout"
	"health/seeds"
	"health/service/links"
	"log"
	"os"
	"time"

	"gorm.io/driver/postgres"
	"gorm.io/gorm"
	dblogger "gorm.io/gorm/logger"
)

// domainModels lista os models migrados; lgpd_purge_test.go usa a mesma lista para exigir
// que a função lgpd_purge_user cubra todas as tabelas de domínio.
var domainModels = []interface{}{
	&workout.Exercise{},
	&workout.Workout{},
	&workout.ExerciseWorkout{},
	&diet.Meal{},
	&diet.Food{},
	&diet.MealFood{},
	&config.UserConfig{},
	&config.History{},
	&link.StudentProfessional{},
}

func InitDatabase() (*gorm.DB, error) {

	logger = GetLogger("PG")

	databaseURL := os.Getenv("DATABASE_URL")

	if databaseURL == "" {
		return nil, fmt.Errorf("DATABASE_URL environment variable not configured")
	}

	dbLogger := dblogger.New(
		log.New(os.Stdout, "\r\n", log.LstdFlags),
		dblogger.Config{
			SlowThreshold:             time.Second,
			LogLevel:                  dblogger.Warn,
			IgnoreRecordNotFoundError: true,
			Colorful:                  true,
		},
	)

	db, err := gorm.Open(postgres.Open(databaseURL), &gorm.Config{
		Logger: dbLogger,
	})

	if err != nil {
		logger.ErrF("Postgres connection failed: %s", err)
		return nil, err
	}

	if err := prepareOwnershipMigration(db); err != nil {
		return nil, err
	}

	for _, table := range domainModels {
		if err := db.AutoMigrate(table); err != nil {
			return nil, err
		}
	}

	if err := installLgpdPurge(db); err != nil {
		return nil, fmt.Errorf("install lgpd purge function: %w", err)
	}

	links.Init(db)

	seeds.Load(db)

	return db, nil
}
