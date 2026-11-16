package config

import (
	"gorm.io/gorm"
)

// prepareOwnershipMigration moves the workout/meal ownership from the legacy
// user column to student_id/author_id before AutoMigrate enforces NOT NULL.
// Existing rows keep working: student_id = author_id = old user id.
func prepareOwnershipMigration(db *gorm.DB) error {
	if err := migrateOwnership(db, "workouts", []string{
		`DROP INDEX IF EXISTS idx_workouts_user_day`,
	}); err != nil {
		return err
	}
	return migrateOwnership(db, "meals", nil)
}

func migrateOwnership(db *gorm.DB, table string, extra []string) error {
	if !db.Migrator().HasTable(table) {
		return nil
	}

	steps := []string{
		`ALTER TABLE ` + table + ` ADD COLUMN IF NOT EXISTS student_id uuid`,
		`ALTER TABLE ` + table + ` ADD COLUMN IF NOT EXISTS author_id uuid`,
	}

	legacy := ""
	switch {
	case db.Migrator().HasColumn(table, "user_id"):
		legacy = "user_id"
	case db.Migrator().HasColumn(table, "user"):
		legacy = `"user"`
	}
	if legacy != "" {
		steps = append(steps,
			`UPDATE `+table+` SET student_id = `+legacy+` WHERE student_id IS NULL`,
			`UPDATE `+table+` SET author_id = `+legacy+` WHERE author_id IS NULL`,
		)
	}

	steps = append(steps, extra...)
	steps = append(steps,
		`ALTER TABLE `+table+` DROP COLUMN IF EXISTS user_id`,
		`ALTER TABLE `+table+` DROP COLUMN IF EXISTS "user"`,
		`ALTER TABLE `+table+` ALTER COLUMN student_id SET NOT NULL`,
		`ALTER TABLE `+table+` ALTER COLUMN author_id SET NOT NULL`,
	)

	for _, step := range steps {
		if err := db.Exec(step).Error; err != nil {
			return err
		}
	}
	return nil
}
