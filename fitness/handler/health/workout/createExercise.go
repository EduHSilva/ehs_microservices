package workout

import (
	"errors"
	"net/http"
	"strings"
	"unicode/utf8"

	"health/helper"
	"health/schemas/workout"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"github.com/nicksnyder/go-i18n/v2/i18n"
	"gorm.io/gorm"
)

var allowedBodyParts = map[string]bool{
	"upper arms": true,
	"upper legs": true,
	"lower arms": true,
	"lower legs": true,
	"back":       true,
	"waist":      true,
	"chest":      true,
	"shoulders":  true,
	"cardio":     true,
	"neck":       true,
}

// CreateExerciseHandler cria um exercício na biblioteca pessoal do usuário.
// @Summary Cria um exercício próprio
// @Description O exercício fica visível apenas para o usuário que o criou.
// @Tags Exercícios
// @Accept json
// @Produce json
// @Param request body CreateExerciseRequest true "Exercício"
// @Success 200 {object} ResponseExercises
// @Failure 400 {object} helper.ErrorResponse
// @Failure 401 {object} helper.ErrorResponse
// @Router /workout/exercises [post]
func CreateExerciseHandler(ctx *gin.Context) {
	getI18n, _ := ctx.Get("i18n")
	locale, _ := ctx.Get("locale")
	localizer := getI18n.(*i18n.Localizer)

	caller, exists := helper.GatewayCaller(ctx)
	if !exists {
		helper.SendErrorDefault(ctx, http.StatusUnauthorized, localizer)
		return
	}

	request := CreateExerciseRequest{}
	if err := ctx.BindJSON(&request); err != nil {
		helper.SendError(ctx, http.StatusBadRequest, err.Error())
		return
	}

	name := strings.TrimSpace(request.Name)
	if name == "" || utf8.RuneCountInString(name) > 80 {
		helper.SendError(ctx, http.StatusBadRequest, localizer.MustLocalize(&i18n.LocalizeConfig{MessageID: "invalidExerciseName"}))
		return
	}

	bodyPart := strings.ToLower(strings.TrimSpace(request.BodyPart))
	if !allowedBodyParts[bodyPart] {
		helper.SendError(ctx, http.StatusBadRequest, localizer.MustLocalize(&i18n.LocalizeConfig{MessageID: "invalidExerciseBodyPart"}))
		return
	}

	var existing workout.Exercise
	err := db.Where("author_id = ? AND lower(name_pt) = lower(?)", caller.ID, name).First(&existing).Error
	if err == nil {
		helper.SendError(ctx, http.StatusBadRequest, localizer.MustLocalize(&i18n.LocalizeConfig{MessageID: "alreadyExists"}))
		return
	}
	if !errors.Is(err, gorm.ErrRecordNotFound) {
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, localizer)
		return
	}

	exercise := workout.Exercise{
		Name:       name,
		NamePt:     name,
		BodyPart:   bodyPart,
		ExternalID: "custom:" + uuid.NewString(),
		AuthorID:   caller.ID,
	}
	if err := db.Create(&exercise).Error; err != nil {
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, localizer)
		return
	}

	responseName := exercise.Name
	if locale == "pt_BR" {
		responseName = exercise.NamePt
	}

	helper.SendSuccess(ctx, ResponseDataExercise{
		ID:       exercise.ID,
		Name:     responseName,
		BodyPart: exercise.BodyPart,
		Custom:   true,
	})
}
