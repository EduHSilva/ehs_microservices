package workout

import (
	"errors"
	"net/http"
	"strings"
	"unicode/utf8"

	"health/helper"
	"health/schemas/workout"

	"github.com/gin-gonic/gin"
	"github.com/nicksnyder/go-i18n/v2/i18n"
	"gorm.io/gorm"
)

// UpdateExerciseHandler edita um exercício da biblioteca pessoal do usuário.
// @Summary Edita um exercício próprio
// @Description Apenas o autor edita o exercício; ele continua visível só para ele.
// @Tags Exercícios
// @Accept json
// @Produce json
// @Param id query int true "ID do exercício"
// @Param request body CreateExerciseRequest true "Exercício"
// @Success 200 {object} ResponseExercises
// @Failure 400 {object} helper.ErrorResponse
// @Failure 401 {object} helper.ErrorResponse
// @Failure 404 {object} helper.ErrorResponse
// @Router /workout/exercises [put]
func UpdateExerciseHandler(ctx *gin.Context) {
	getI18n, _ := ctx.Get("i18n")
	locale, _ := ctx.Get("locale")
	localizer := getI18n.(*i18n.Localizer)

	id := ctx.Query("id")
	if id == "" {
		helper.SendError(ctx, http.StatusBadRequest,
			helper.ErrParamIsRequired("id", "query param").Error())
		return
	}

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

	exercise := workout.Exercise{}
	err := db.Where("id = ? AND author_id = ?", id, caller.ID).First(&exercise).Error
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			helper.SendErrorDefault(ctx, http.StatusNotFound, localizer)
			return
		}
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, localizer)
		return
	}

	var duplicate workout.Exercise
	err = db.Where("author_id = ? AND lower(name_pt) = lower(?) AND id <> ?", caller.ID, name, exercise.ID).
		First(&duplicate).Error
	if err == nil {
		helper.SendError(ctx, http.StatusBadRequest, localizer.MustLocalize(&i18n.LocalizeConfig{MessageID: "alreadyExists"}))
		return
	}
	if !errors.Is(err, gorm.ErrRecordNotFound) {
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, localizer)
		return
	}

	exercise.Name = name
	exercise.NamePt = name
	exercise.BodyPart = bodyPart
	if err := db.Save(&exercise).Error; err != nil {
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
