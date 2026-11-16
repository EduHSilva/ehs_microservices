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

// ListStudentsHandler lista os alunos ativos e os convites pendentes do profissional.
// @Summary Lista alunos e convites
// @Tags Alunos
// @Produce json
// @Param domain query string false "PERSONAL ou NUTRITIONIST"
// @Success 200 {object} StudentsResponse
// @Failure 400 {object} helper.ErrorResponse
// @Failure 401 {object} helper.ErrorResponse
// @Failure 403 {object} helper.ErrorResponse
// @Router /students [get]
func ListStudentsHandler(ctx *gin.Context) {
	caller, exists := helper.GatewayCaller(ctx)
	if !exists {
		helper.SendErrorDefault(ctx, http.StatusUnauthorized, localizerOf(ctx))
		return
	}
	if !caller.IsProfessional() {
		helper.SendErrorDefault(ctx, http.StatusForbidden, localizerOf(ctx))
		return
	}

	domain := link.Domain(strings.ToUpper(strings.TrimSpace(ctx.Query("domain"))))
	if raw := strings.TrimSpace(ctx.Query("domain")); raw != "" && !domain.Valid() {
		sendLocalizedError(ctx, http.StatusBadRequest, "invalidDomain")
		return
	}

	_ = links.ExpireDue(time.Now())

	relations, err := links.ActiveForProfessionalList(caller.ID, domain)
	if err != nil {
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, localizerOf(ctx))
		return
	}

	ids := make([]string, 0, len(relations))
	for _, relation := range relations {
		ids = append(ids, stringOrEmpty(relation.StudentID))
	}
	profiles := map[string]users.Profile{}
	if users.Enabled() {
		if profiles, err = users.List(ids); err != nil {
			profiles = map[string]users.Profile{}
		}
	}

	students := make([]StudentCard, 0, len(relations))
	for _, relation := range relations {
		if !caller.HasRole(relation.Domain.Role()) {
			_ = links.EndActive(relation.ID, "SYSTEM")
			continue
		}
		profile := profiles[stringOrEmpty(relation.StudentID)]
		students = append(students, ConvertLinkToStudentCard(relation, profile))
	}

	pending, err := links.PendingForProfessional(caller.ID)
	if err != nil {
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, localizerOf(ctx))
		return
	}
	invites := make([]InviteCard, 0, len(pending))
	for _, relation := range pending {
		invites = append(invites, ConvertLinkToInviteCard(relation))
	}

	helper.SendSuccess(ctx, StudentsResponse{Students: students, Invites: invites})
}

// GetStudentHandler devolve o cartão de um vínculo ativo.
// @Summary Detalha um vínculo
// @Description O id é o link_id devolvido em GET /students.
// @Tags Alunos
// @Produce json
// @Param id path int true "link_id"
// @Success 200 {object} StudentCard
// @Failure 401 {object} helper.ErrorResponse
// @Failure 404 {object} helper.ErrorResponse
// @Router /students/{id} [get]
func GetStudentHandler(ctx *gin.Context) {
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
	if !found || relation.Status != link.StatusActive {
		helper.SendErrorDefault(ctx, http.StatusNotFound, localizerOf(ctx))
		return
	}
	if relation.StudentID == nil {
		helper.SendErrorDefault(ctx, http.StatusNotFound, localizerOf(ctx))
		return
	}

	profile, _, err := users.Get(*relation.StudentID)
	if err != nil && users.Enabled() {
		helper.SendErrorDefault(ctx, http.StatusServiceUnavailable, localizerOf(ctx))
		return
	}

	helper.SendSuccess(ctx, ConvertLinkToStudentCard(relation, profile))
}
