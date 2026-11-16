package student

import (
	"net/http"
	"strings"
	"time"

	"health/helper"
	"health/schemas/link"

	"github.com/gin-gonic/gin"
)

// UpdateLinkHandler atualiza o objetivo do vínculo.
// @Summary Atualiza o objetivo
// @Description Somente o profissional dono do vínculo ativo pode editar o objetivo.
// @Tags Alunos
// @Accept json
// @Produce json
// @Param id path int true "link_id"
// @Param request body UpdateLinkRequest true "Objetivo"
// @Success 200 {object} StudentCard
// @Failure 400 {object} helper.ErrorResponse
// @Failure 401 {object} helper.ErrorResponse
// @Failure 403 {object} helper.ErrorResponse
// @Failure 404 {object} helper.ErrorResponse
// @Router /students/links/{id} [patch]
func UpdateLinkHandler(ctx *gin.Context) {
	request := UpdateLinkRequest{}
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
	if relation.ProfessionalID != caller.ID || !caller.HasRole(relation.Domain.Role()) {
		helper.SendErrorDefault(ctx, http.StatusForbidden, localizerOf(ctx))
		return
	}

	goal := strings.TrimSpace(*request.Goal)
	if err := db.Model(&link.StudentProfessional{}).
		Where("id = ?", relation.ID).
		Update("goal", goal).Error; err != nil {
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, localizerOf(ctx))
		return
	}
	relation.Goal = goal

	helper.SendSuccess(ctx, ConvertLinkToStudentCard(relation, profileOf(relation)))
}

// EndLinkHandler encerra um vínculo ativo por qualquer um dos lados.
// @Summary Encerra um vínculo
// @Description O aluno mantém o plano; o profissional perde o acesso.
// @Tags Alunos
// @Produce json
// @Param id path int true "link_id"
// @Success 200 {object} StudentCard
// @Failure 401 {object} helper.ErrorResponse
// @Failure 404 {object} helper.ErrorResponse
// @Router /students/links/{id} [delete]
func EndLinkHandler(ctx *gin.Context) {
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

	endedBy := endedByProfessional
	if relation.StudentID != nil && *relation.StudentID == caller.ID {
		endedBy = endedByStudent
	}

	now := time.Now()
	if err := db.Model(&link.StudentProfessional{}).
		Where("id = ? AND status = ?", relation.ID, link.StatusActive).
		Updates(map[string]interface{}{
			"status":   link.StatusEnded,
			"ended_at": now,
			"ended_by": endedBy,
		}).Error; err != nil {
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, localizerOf(ctx))
		return
	}
	relation.Status = link.StatusEnded
	relation.EndedAt = &now
	relation.EndedBy = endedBy

	helper.SendSuccess(ctx, ConvertLinkToStudentCard(relation, profileOf(relation)))
}
