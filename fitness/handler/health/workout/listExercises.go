package workout

import (
	"net/http"

	"health/helper"
	"health/schemas/workout"

	"github.com/gin-gonic/gin"
	"github.com/nicksnyder/go-i18n/v2/i18n"
)

// GetExercisesHandler lista o catálogo global de exercícios.
// @Summary Lista o catálogo de exercícios
// @Description As imagens externas são cacheadas e baixadas em segundo plano.
// @Tags Exercícios
// @Produce json
// @Success 200 {object} map[string]interface{}
// @Failure 401 {object} helper.ErrorResponse
// @Router /workout/exercises [get]
func GetExercisesHandler(ctx *gin.Context) {
	exercises := make([]workout.Exercise, 0)
	exercisesResponses := make([]ResponseDataExercise, 0)

	getI18n, _ := ctx.Get("i18n")
	locale, _ := ctx.Get("locale")
	localizer := getI18n.(*i18n.Localizer)

	if err := db.Order("name").Find(&exercises).Error; err != nil {
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, localizer)
		return
	}

	// Missing images are fetched in the background and cached; the catalog
	// response never waits for the upstream API.
	scheduleExerciseImages(exercises)

	for _, exercise := range exercises {
		translatedName := exercise.Name
		translatedInstructions := exercise.Instructions

		if locale == "pt_BR" {
			translatedName = exercise.NamePt
			translatedInstructions = exercise.InstructionsPt
		}

		exercisesResponses = append(exercisesResponses, ResponseDataExercise{
			ID:           exercise.ID,
			Name:         translatedName,
			BodyPart:     exercise.BodyPart,
			Target:       exercise.Target,
			Instructions: translatedInstructions,
			ImageUrl:     cachedExerciseImage(exercise),
		})
	}

	helper.SendSuccess(ctx, exercisesResponses)
}
