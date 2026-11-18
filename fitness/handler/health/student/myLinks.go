package student

import (
	"net/http"
	"time"

	"health/helper"
	"health/integration/users"
	"health/service/links"

	"github.com/gin-gonic/gin"
)

// ListMyLinksHandler lista os vínculos ativos do próprio aluno.
// @Summary Lista meus vínculos ativos
// @Description Usado pelo aluno para saber quem gerencia cada domínio do plano.
// @Tags Alunos
// @Produce json
// @Success 200 {array} MyLinkCard
// @Failure 401 {object} helper.ErrorResponse
// @Failure 403 {object} helper.ErrorResponse
// @Router /students/links [get]
func ListMyLinksHandler(ctx *gin.Context) {
	caller, exists := helper.GatewayCaller(ctx)
	if !exists {
		helper.SendErrorDefault(ctx, http.StatusUnauthorized, localizerOf(ctx))
		return
	}
	if !caller.IsStudent() {
		helper.SendErrorDefault(ctx, http.StatusForbidden, localizerOf(ctx))
		return
	}

	_ = links.ExpireDue(time.Now())

	relations := links.ActiveForStudent(caller.ID)

	ids := make([]string, 0, len(relations))
	for _, relation := range relations {
		ids = append(ids, relation.ProfessionalID)
	}
	professionals := map[string]users.Profile{}
	if users.Enabled() && len(ids) > 0 {
		if fetched, err := users.List(ids); err == nil {
			professionals = fetched
		}
	}

	cards := make([]MyLinkCard, 0, len(relations))
	for _, relation := range relations {
		cards = append(cards, ConvertLinkToMyLinkCard(relation, professionals[relation.ProfessionalID]))
	}

	helper.SendSuccess(ctx, cards)
}
