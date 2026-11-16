package config

import (
	"health/schemas/config"
	"net/http"
	"time"

	"health/helper"

	"github.com/gin-gonic/gin"
	"github.com/nicksnyder/go-i18n/v2/i18n"
	"gorm.io/gorm"
)

// GetUserDayHistoryHandler devolve o acompanhamento diário privado de hoje.
// @Summary Lê o acompanhamento do dia
// @Tags Configuração
// @Produce json
// @Success 200 {object} map[string]interface{}
// @Failure 401 {object} helper.ErrorResponse
// @Failure 404 {object} helper.ErrorResponse
// @Router /config/history [get]
func GetUserDayHistoryHandler(ctx *gin.Context) {
	getI18n, _ := ctx.Get("i18n")
	userID, exists := helper.GatewayUserID(ctx)
	if !exists {
		helper.SendErrorDefault(ctx, http.StatusUnauthorized, getI18n.(*i18n.Localizer))
		return
	}

	history := config.History{}
	today := time.Now().Format("2006-01-02")
	if err := db.Where("user_id = ? AND event_date = ?", userID, today).First(&history).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			helper.SendErrorDefault(ctx, http.StatusNotFound, getI18n.(*i18n.Localizer))
			return
		}
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, getI18n.(*i18n.Localizer))
		return
	}

	helper.SendSuccess(ctx, ConvertHistoryToHistoryResponse(&history))
}
