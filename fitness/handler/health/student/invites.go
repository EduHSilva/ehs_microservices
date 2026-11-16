package student

import (
	"net/http"
	"time"

	"health/helper"
	"health/integration/users"
	"health/schemas/link"
	"health/service/links"

	"github.com/gin-gonic/gin"
)

// ListInvitesHandler lista convites: enviados (profissional) ou recebidos (aluno).
// @Summary Lista convites pendentes
// @Tags Alunos
// @Produce json
// @Success 200 {object} map[string]interface{}
// @Failure 401 {object} helper.ErrorResponse
// @Failure 403 {object} helper.ErrorResponse
// @Router /students/invites [get]
func ListInvitesHandler(ctx *gin.Context) {
	caller, exists := helper.GatewayCaller(ctx)
	if !exists {
		helper.SendErrorDefault(ctx, http.StatusUnauthorized, localizerOf(ctx))
		return
	}

	_ = links.ExpireDue(time.Now())

	if caller.IsProfessional() {
		pending, err := links.PendingForProfessional(caller.ID)
		if err != nil {
			helper.SendErrorDefault(ctx, http.StatusInternalServerError, localizerOf(ctx))
			return
		}
		cards := make([]InviteCard, 0, len(pending))
		for _, relation := range pending {
			cards = append(cards, ConvertLinkToInviteCard(relation))
		}
		helper.SendSuccess(ctx, cards)
		return
	}

	if !caller.IsStudent() {
		helper.SendErrorDefault(ctx, http.StatusForbidden, localizerOf(ctx))
		return
	}

	profile, denied := callerProfile(caller)
	if denied != nil {
		helper.SendErrorDefault(ctx, *denied, localizerOf(ctx))
		return
	}

	pending, err := links.PendingByEmail(normalizeEmail(profile.Email))
	if err != nil {
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, localizerOf(ctx))
		return
	}

	professionals := map[string]users.Profile{}
	if users.Enabled() && len(pending) > 0 {
		ids := make([]string, 0, len(pending))
		for _, relation := range pending {
			ids = append(ids, relation.ProfessionalID)
		}
		if fetched, fetchErr := users.List(ids); fetchErr == nil {
			professionals = fetched
		}
	}

	cards := make([]IncomingInviteCard, 0, len(pending))
	for _, relation := range pending {
		cards = append(cards, ConvertLinkToIncomingInvite(relation, professionals[relation.ProfessionalID]))
	}

	helper.SendSuccess(ctx, cards)
}

// ResendInviteHandler reenvia um convite pendente ou expirado.
// @Summary Reenvia um convite
// @Tags Alunos
// @Produce json
// @Param id path int true "ID do convite"
// @Success 200 {object} InviteCard
// @Failure 400 {object} helper.ErrorResponse
// @Failure 401 {object} helper.ErrorResponse
// @Failure 404 {object} helper.ErrorResponse
// @Router /students/invites/{id}/resend [post]
func ResendInviteHandler(ctx *gin.Context) {
	caller, exists := helper.GatewayCaller(ctx)
	if !exists {
		helper.SendErrorDefault(ctx, http.StatusUnauthorized, localizerOf(ctx))
		return
	}

	id := parseUintParam(ctx.Param("id"))
	if id == 0 {
		sendLocalizedError(ctx, http.StatusBadRequest, "invalidId")
		return
	}

	relation, found := ownedLink(id, caller)
	if !found || relation.ProfessionalID != caller.ID {
		helper.SendErrorDefault(ctx, http.StatusNotFound, localizerOf(ctx))
		return
	}
	if relation.Status != link.StatusPending && relation.Status != link.StatusExpired {
		sendLocalizedError(ctx, http.StatusBadRequest, "invalidInvite")
		return
	}

	token, err := newToken()
	if err != nil {
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, localizerOf(ctx))
		return
	}

	now := time.Now()
	relation.Token = token
	relation.Status = link.StatusPending
	relation.ExpiresAt = now.Add(inviteTTL())

	if err := db.Model(&link.StudentProfessional{}).
		Where("id = ?", relation.ID).
		Updates(map[string]interface{}{
			"token":      relation.Token,
			"status":     relation.Status,
			"expires_at": relation.ExpiresAt,
		}).Error; err != nil {
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, localizerOf(ctx))
		return
	}

	sendInviteEmail(relation, professionalName(caller))

	helper.SendSuccess(ctx, ConvertLinkToInviteCard(relation))
}

// RevokeInviteHandler revoga um convite pendente.
// @Summary Revoga um convite
// @Tags Alunos
// @Produce json
// @Param id path int true "ID do convite"
// @Success 200 {object} InviteCard
// @Failure 401 {object} helper.ErrorResponse
// @Failure 404 {object} helper.ErrorResponse
// @Router /students/invites/{id} [delete]
func RevokeInviteHandler(ctx *gin.Context) {
	caller, exists := helper.GatewayCaller(ctx)
	if !exists {
		helper.SendErrorDefault(ctx, http.StatusUnauthorized, localizerOf(ctx))
		return
	}

	id := parseUintParam(ctx.Param("id"))
	if id == 0 {
		sendLocalizedError(ctx, http.StatusBadRequest, "invalidId")
		return
	}

	relation, found := ownedLink(id, caller)
	if !found || relation.ProfessionalID != caller.ID || relation.Status != link.StatusPending {
		helper.SendErrorDefault(ctx, http.StatusNotFound, localizerOf(ctx))
		return
	}

	if err := db.Model(&link.StudentProfessional{}).
		Where("id = ? AND status = ?", relation.ID, link.StatusPending).
		Update("status", link.StatusRevoked).Error; err != nil {
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, localizerOf(ctx))
		return
	}
	relation.Status = link.StatusRevoked

	helper.SendSuccess(ctx, ConvertLinkToInviteCard(relation))
}
