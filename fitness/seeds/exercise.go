package seeds

import (
	"encoding/json"
	"fmt"
	"html/template"
	"log"
	"strings"

	"health/schemas/workout"

	"gorm.io/gorm"
)

type ExerciseInput struct {
	Name           string   `json:"name"`
	NamePt         string   `json:"name_pt"`
	BodyPart       string   `json:"body_part"`
	Instructions   []string `json:"instructions"`
	InstructionsPt []string `json:"instructions_pt"`
	GifURL         string   `json:"gif_url"`
	ID             string   `json:"id"`
	Target         string   `json:"target"`
}

func instructionsToHTML(instructions []string) string {
	var htmlInstructions strings.Builder
	for _, instr := range instructions {
		htmlInstructions.WriteString(fmt.Sprintf("<p>%s</p>", template.HTMLEscapeString(instr)))
	}
	return htmlInstructions.String()
}

func loadExercisesFromFile(db *gorm.DB) error {
	file, err := Files.ReadFile("data/exercises.json")
	if err != nil {
		return err
	}

	var inputs []ExerciseInput
	err = json.Unmarshal(file, &inputs)
	if err != nil {
		return err
	}

	for _, input := range inputs {
		exercise := workout.Exercise{
			Name:           input.Name,
			NamePt:         input.NamePt,
			BodyPart:       input.BodyPart,
			Instructions:   instructionsToHTML(input.Instructions),
			InstructionsPt: instructionsToHTML(input.InstructionsPt),
			ExternalID:     input.ID,
			Target:         input.Target,
			ImageURL:       input.GifURL,
		}
		if err := db.Create(&exercise).Error; err != nil {
			return err
		}
	}

	return nil
}

func loadExerciseFromFile(db *gorm.DB) {
	err := loadExercisesFromFile(db)
	if err != nil {
		log.Fatalf("Error loading exercises from JSON: %v", err)
	}
}
