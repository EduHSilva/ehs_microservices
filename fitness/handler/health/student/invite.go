package student

import (
	"net/http"
	"net/mail"
	"strings"
	"time"

	"health/helper"
	"health/integration/users"
	"health/schemas/link"
	"health/service/links"

	"github.com/gin-gonic/gin"
)

// InviteStudentHandler envia um convite por e-mail a um aluno.
// @Summary Convida um aluno
// @Description O profissional precisa da role do domínio (PERSONAL/NUTRITIONIST). O vínculo só passa a valer após o aceite.
// @Tags Alunos
// @Accept json
// @Produce json
// @Param request body InviteStudentRequest true "Convite"
// @Success 200 {object} InviteCard
// @Failure 400 {object} helper.ErrorResponse
// @Failure 401 {object} helper.ErrorResponse
// @Failure 403 {object} helper.ErrorResponse
// @Failure 409 {object} helper.ErrorResponse
// @Router /students [post]
func InviteStudentHandler(ctx *gin.Context) {
	request := InviteStudentRequest{}
	if err := ctx.ShouldBindJSON(&request); err != nil {
		helper.SendError(ctx, http.StatusBadRequest, err.Error())
		return
	}
	if err := request.Validate(); err != nil {
		helper.SendError(ctx, http.StatusBadRequest, err.Error())
		return
	}

	caller, exists := helper.GatewayCaller(ctx)
	if !exists {
		helper.SendErrorDefault(ctx, http.StatusUnauthorized, localizerOf(ctx))
		return
	}

	domain, denied := professionalDomain(caller, request.Domain)
	if denied != nil {
		if *denied == http.StatusBadRequest {
			sendLocalizedError(ctx, *denied, "invalidDomain")
		} else {
			helper.SendErrorDefault(ctx, *denied, localizerOf(ctx))
		}
		return
	}

	email := normalizeEmail(request.Email)
	if !validEmail(email) {
		sendLocalizedError(ctx, http.StatusBadRequest, "invalidEmail")
		return
	}
	if email == callerEmail(caller) {
		sendLocalizedError(ctx, http.StatusBadRequest, "ownInvite")
		return
	}

	if _, pending := links.PendingFor(caller.ID, email, domain); pending {
		sendLocalizedError(ctx, http.StatusConflict, "inviteExists")
		return
	}

	profile, found, err := users.GetByEmail(email)
	if err != nil && users.Enabled() {
		helper.SendErrorDefault(ctx, http.StatusServiceUnavailable, localizerOf(ctx))
		return
	}
	if found && profile.ID == caller.ID {
		sendLocalizedError(ctx, http.StatusBadRequest, "ownInvite")
		return
	}
	if found && (profile.HasRole(helper.RolePersonal) || profile.HasRole(helper.RoleNutritionist)) {
		sendLocalizedError(ctx, http.StatusConflict, "inviteProfessional")
		return
	}
	if found && links.StudentHasActiveDomain(profile.ID, domain) {
		sendLocalizedError(ctx, http.StatusConflict, "alreadyLinked")
		return
	}

	token, err := newToken()
	if err != nil {
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, localizerOf(ctx))
		return
	}

	invite := link.StudentProfessional{
		ProfessionalID: caller.ID,
		Domain:         domain,
		Goal:           strings.TrimSpace(request.Goal),
		Status:         link.StatusPending,
		InvitedEmail:   email,
		Token:          token,
		ExpiresAt:      time.Now().Add(inviteTTL()),
	}
	if found {
		invite.StudentID = &profile.ID
		invite.StudentName = profile.Username
	}

	if err := db.Create(&invite).Error; err != nil {
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, localizerOf(ctx))
		return
	}

	sendInviteEmail(invite, professionalName(caller))

	helper.SendSuccess(ctx, ConvertLinkToInviteCard(invite))
}

func validEmail(email string) bool {
	if email == "" || len(email) > 255 {
		return false
	}
	address, err := mail.ParseAddress(email)
	return err == nil && strings.EqualFold(address.Address, email)
}

// callerEmail resolves the caller's e-mail when available, so the professional
// cannot invite their own account.
func callerEmail(caller helper.Caller) string {
	profile, found, err := users.Get(caller.ID)
	if err != nil || !found {
		return ""
	}
	return normalizeEmail(profile.Email)
}

func professionalName(caller helper.Caller) string {
	profile, found, err := users.Get(caller.ID)
	if err != nil || !found || profile.Username == "" {
		return "Um profissional"
	}
	return profile.Username
}
