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

	tables := []interface{}{
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

	if err := prepareOwnershipMigration(db); err != nil {
		return nil, err
	}

	for _, table := range tables {
		if err := db.AutoMigrate(table); err != nil {
			return nil, err
		}
	}

	links.Init(db)

	seeds.Load(db)

	return db, nil
}
