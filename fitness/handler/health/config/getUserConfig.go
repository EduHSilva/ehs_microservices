package config

import (
	"net/http"

	"health/helper"
	configSchema "health/schemas/config"
	"health/service/access"

	"github.com/gin-gonic/gin"
	"github.com/nicksnyder/go-i18n/v2/i18n"
)

// GetUserConfigHandler devolve a configuração privada do usuário.
// @Summary Lê a configuração do usuário
// @Description Cria a configuração padrão no primeiro acesso (showCreatine=true).
// @Description Com student_id, exige vínculo ativo com o profissional (qualquer domínio).
// @Tags Configuração
// @Produce json
// @Param student_id query string false "Aluno (ausente = o próprio usuário)"
// @Success 200 {object} map[string]interface{}
// @Failure 401 {object} helper.ErrorResponse
// @Failure 404 {object} helper.ErrorResponse
// @Router /config/user [get]
func GetUserConfigHandler(ctx *gin.Context) {
	getI18n, _ := ctx.Get("i18n")
	locale, _ := ctx.Get("locale")
	localizer := getI18n.(*i18n.Localizer)

	caller, exists := helper.GatewayCaller(ctx)
	if !exists {
		helper.SendErrorDefault(ctx, http.StatusUnauthorized, localizer)
		return
	}

	userID, denied := access.AnyDomainTarget(caller, helper.RequestedStudent(ctx, ""))
	if denied != nil {
		helper.SendErrorDefault(ctx, denied.Status, localizer)
		return
	}

	userConfig := configSchema.UserConfig{UserID: userID, ShowCreatine: true}
	if err := db.Where("user_id = ?", userID).FirstOrCreate(&userConfig).Error; err != nil {
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, localizer)
		return
	}

	helper.SendSuccess(ctx, ConvertUserConfigToUserConfigResponse(&userConfig, locale))
}
