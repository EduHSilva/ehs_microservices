package config

import (
	"net/http"

	"health/helper"
	configSchema "health/schemas/config"

	"github.com/gin-gonic/gin"
	"github.com/nicksnyder/go-i18n/v2/i18n"
)

// GetUserConfigHandler devolve a configuração privada do usuário.
// @Summary Lê a configuração do usuário
// @Description Cria a configuração padrão no primeiro acesso (showCreatine=true).
// @Tags Configuração
// @Produce json
// @Success 200 {object} map[string]interface{}
// @Failure 401 {object} helper.ErrorResponse
// @Router /config/user [get]
func GetUserConfigHandler(ctx *gin.Context) {
	getI18n, _ := ctx.Get("i18n")
	locale, _ := ctx.Get("locale")

	userID, exists := helper.GatewayUserID(ctx)
	if !exists {
		helper.SendErrorDefault(ctx, http.StatusUnauthorized, getI18n.(*i18n.Localizer))
		return
	}

	userConfig := configSchema.UserConfig{UserID: userID, ShowCreatine: true}
	if err := db.Where("user_id = ?", userID).FirstOrCreate(&userConfig).Error; err != nil {
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, getI18n.(*i18n.Localizer))
		return
	}

	helper.SendSuccess(ctx, ConvertUserConfigToUserConfigResponse(&userConfig, locale))
}
