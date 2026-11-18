package config

import (
	"health/helper"
	configSchema "health/schemas/config"
	"health/service/access"
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/nicksnyder/go-i18n/v2/i18n"
	"gorm.io/datatypes"
)

// UpdateUserConfigHandler atualiza a configuração privada do usuário.
// @Summary Atualiza a configuração do usuário
// @Description Com student_id, qualquer profissional com vínculo ativo pode ajustar.
// @Tags Configuração
// @Accept json
// @Produce json
// @Param request body UpdateUserConfigRequest true "Configuração"
// @Success 200 {object} map[string]interface{}
// @Failure 400 {object} helper.ErrorResponse
// @Failure 401 {object} helper.ErrorResponse
// @Failure 403 {object} helper.ErrorResponse
// @Failure 404 {object} helper.ErrorResponse
// @Router /config/user [put]
func UpdateUserConfigHandler(ctx *gin.Context) {
	request := UpdateUserConfigRequest{}
	if err := ctx.ShouldBindJSON(&request); err != nil {
		helper.SendError(ctx, http.StatusBadRequest, err.Error())
		return
	}
	if err := request.Validate(); err != nil {
		helper.SendError(ctx, http.StatusBadRequest, err.Error())
		return
	}

	getI18n, _ := ctx.Get("i18n")
	localizer := getI18n.(*i18n.Localizer)
	caller, exists := helper.GatewayCaller(ctx)
	if !exists {
		helper.SendErrorDefault(ctx, http.StatusUnauthorized, localizer)
		return
	}

	userID, denied := access.AnyDomainTarget(caller, helper.RequestedStudent(ctx, request.StudentID))
	if denied != nil {
		helper.SendErrorDefault(ctx, denied.Status, localizer)
		return
	}

	userConfig := configSchema.UserConfig{UserID: userID}
	if err := db.Where("user_id = ?", userID).FirstOrCreate(&userConfig).Error; err != nil {
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, localizer)
		return
	}
	if request.ShowCreatine != nil {
		userConfig.ShowCreatine = *request.ShowCreatine
	}
	if request.DailyCardio != nil {
		userConfig.DailyCardio = *request.DailyCardio
	}
	if request.ShowWorkout != nil {
		userConfig.ShowWorkout = *request.ShowWorkout
	}
	if err := db.Save(&userConfig).Error; err != nil {
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, localizer)
		return
	}

	helper.SendSuccess(ctx, ConvertUserConfigToUserConfigResponse(&userConfig, nil))
}

// UpdateUserDayHistoryHandler grava o acompanhamento diário privado de hoje.
// @Summary Atualiza o acompanhamento do dia
// @Tags Configuração
// @Accept json
// @Produce json
// @Param request body UpdateHistoryRequest true "Acompanhamento"
// @Success 200 {object} map[string]interface{}
// @Failure 400 {object} helper.ErrorResponse
// @Failure 401 {object} helper.ErrorResponse
// @Router /config/history [put]
func UpdateUserDayHistoryHandler(ctx *gin.Context) {
	request := UpdateHistoryRequest{}
	if err := ctx.ShouldBindJSON(&request); err != nil {
		helper.SendError(ctx, http.StatusBadRequest, err.Error())
		return
	}
	if err := request.Validate(); err != nil {
		helper.SendError(ctx, http.StatusBadRequest, err.Error())
		return
	}

	getI18n, _ := ctx.Get("i18n")
	userID, exists := helper.GatewayUserID(ctx)
	if !exists {
		helper.SendErrorDefault(ctx, http.StatusUnauthorized, getI18n.(*i18n.Localizer))
		return
	}

	today := time.Now()
	history := configSchema.History{UserID: userID, EventDate: datatypes.Date(today)}
	if err := db.Where("user_id = ? AND event_date = ?", userID, today.Format("2006-01-02")).FirstOrCreate(&history).Error; err != nil {
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, getI18n.(*i18n.Localizer))
		return
	}
	if request.Creatine != nil {
		history.Creatine = *request.Creatine
	}
	if request.Cardio != nil {
		history.Cardio = *request.Cardio
	}
	if request.Trained != nil {
		history.Trained = *request.Trained
	}
	if err := db.Save(&history).Error; err != nil {
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, getI18n.(*i18n.Localizer))
		return
	}

	helper.SendSuccess(ctx, ConvertHistoryToHistoryResponse(&history))
}
