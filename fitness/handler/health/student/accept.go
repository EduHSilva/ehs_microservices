package student

import (
	"net/http"
	"strings"
	"time"

	"health/helper"
	"health/integration/users"
	"health/schemas/link"
	"health/service/links"

	"github.com/gin-gonic/gin"
)

// AcceptInviteHandler aceita um convite.
// @Summary Aceita um convite
// @Description Só a conta HEALTH_USER com o e-mail convidado pode aceitar; quem já tem profissional no domínio precisa encerrar o vínculo anterior.
// @Tags Alunos
// @Accept json
// @Produce json
// @Param request body InviteTokenRequest true "Token do convite"
// @Success 200 {object} InviteCard
// @Failure 400 {object} helper.ErrorResponse
// @Failure 401 {object} helper.ErrorResponse
// @Failure 403 {object} helper.ErrorResponse
// @Failure 404 {object} helper.ErrorResponse
// @Failure 409 {object} helper.ErrorResponse
// @Router /students/invites/accept [post]
func AcceptInviteHandler(ctx *gin.Context) {
	relation, caller, ok := inviteContext(ctx)
	if !ok {
		return
	}

	if links.StudentHasActiveDomain(caller.ID, relation.Domain) {
		sendLocalizedError(ctx, http.StatusConflict, "alreadyLinked")
		return
	}

	now := time.Now()
	name := caller.Username
	if name == "" {
		name = relation.InvitedEmail
	}

	studentID := caller.ID
	if err := db.Model(&link.StudentProfessional{}).
		Where("id = ? AND status = ?", relation.ID, link.StatusPending).
		Updates(map[string]interface{}{
			"student_id":   studentID,
			"student_name": name,
			"status":       link.StatusActive,
			"accepted_at":  now,
		}).Error; err != nil {
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, localizerOf(ctx))
		return
	}
	relation.StudentID = &studentID
	relation.StudentName = name
	relation.Status = link.StatusActive
	relation.AcceptedAt = &now

	helper.SendSuccess(ctx, ConvertLinkToInviteCard(relation))
}

// DeclineInviteHandler recusa um convite.
// @Summary Recusa um convite
// @Tags Alunos
// @Accept json
// @Produce json
// @Param request body InviteTokenRequest true "Token do convite"
// @Success 200 {object} InviteCard
// @Failure 400 {object} helper.ErrorResponse
// @Failure 401 {object} helper.ErrorResponse
// @Failure 403 {object} helper.ErrorResponse
// @Failure 404 {object} helper.ErrorResponse
// @Router /students/invites/decline [post]
func DeclineInviteHandler(ctx *gin.Context) {
	relation, _, ok := inviteContext(ctx)
	if !ok {
		return
	}

	if err := db.Model(&link.StudentProfessional{}).
		Where("id = ? AND status = ?", relation.ID, link.StatusPending).
		Update("status", link.StatusDeclined).Error; err != nil {
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, localizerOf(ctx))
		return
	}
	relation.Status = link.StatusDeclined

	helper.SendSuccess(ctx, ConvertLinkToInviteCard(relation))
}

// inviteContext validates the token, the invited account and the invitation
// state shared by accept and decline.
func inviteContext(ctx *gin.Context) (link.StudentProfessional, users.Profile, bool) {
	request := InviteTokenRequest{}
	if err := ctx.ShouldBindJSON(&request); err != nil {
		request.Token = ctx.Query("token")
	}
	if request.Validate() != nil {
		sendLocalizedError(ctx, http.StatusBadRequest, "invalidInvite")
		return link.StudentProfessional{}, users.Profile{}, false
	}

	callerHeader, exists := helper.GatewayCaller(ctx)
	if !exists {
		helper.SendErrorDefault(ctx, http.StatusUnauthorized, localizerOf(ctx))
		return link.StudentProfessional{}, users.Profile{}, false
	}

	relation, found := links.FindByToken(strings.TrimSpace(request.Token))
	if !found {
		helper.SendErrorDefault(ctx, http.StatusNotFound, localizerOf(ctx))
		return link.StudentProfessional{}, users.Profile{}, false
	}
	if relation.Status != link.StatusPending {
		sendLocalizedError(ctx, http.StatusBadRequest, "invalidInvite")
		return link.StudentProfessional{}, users.Profile{}, false
	}
	if relation.Expired(time.Now()) {
		_ = db.Model(&link.StudentProfessional{}).
			Where("id = ? AND status = ?", relation.ID, link.StatusPending).
			Update("status", link.StatusExpired)
		sendLocalizedError(ctx, http.StatusBadRequest, "inviteExpired")
		return link.StudentProfessional{}, users.Profile{}, false
	}

	profile, denied := callerProfile(callerHeader)
	if denied != nil {
		helper.SendErrorDefault(ctx, *denied, localizerOf(ctx))
		return link.StudentProfessional{}, users.Profile{}, false
	}
	if !profile.HasRole(helper.RoleStudent) && !callerHeader.IsStudent() {
		helper.SendErrorDefault(ctx, http.StatusForbidden, localizerOf(ctx))
		return link.StudentProfessional{}, users.Profile{}, false
	}
	if normalizeEmail(profile.Email) != normalizeEmail(relation.InvitedEmail) {
		helper.SendErrorDefault(ctx, http.StatusForbidden, localizerOf(ctx))
		return link.StudentProfessional{}, users.Profile{}, false
	}
	if relation.StudentID != nil && *relation.StudentID != profile.ID {
		helper.SendErrorDefault(ctx, http.StatusForbidden, localizerOf(ctx))
		return link.StudentProfessional{}, users.Profile{}, false
	}

	return relation, profile, true
}
