package config

import (
	"health/helper"
	"health/schemas/config"
	"health/service/access"
	"net/http"
	"strings"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/nicksnyder/go-i18n/v2/i18n"
	"gorm.io/gorm"
)

const historyDateLayout = "2006-01-02"

// GetUserDayHistoryHandler devolve o acompanhamento diário do aluno.
// Sem from/to devolve apenas hoje; com from/to devolve a lista do intervalo (inclusive).
// Com student_id, exige vínculo ativo com o profissional (qualquer domínio) e é somente leitura.
// @Summary Lê o acompanhamento diário
// @Tags Configuração
// @Produce json
// @Param student_id query string false "Aluno (ausente = o próprio usuário)"
// @Param from query string false "Início do intervalo (YYYY-MM-DD)"
// @Param to query string false "Fim do intervalo (YYYY-MM-DD)"
// @Success 200 {object} map[string]interface{}
// @Failure 400 {object} helper.ErrorResponse
// @Failure 401 {object} helper.ErrorResponse
// @Failure 403 {object} helper.ErrorResponse
// @Failure 404 {object} helper.ErrorResponse
// @Router /config/history [get]
func GetUserDayHistoryHandler(ctx *gin.Context) {
	getI18n, _ := ctx.Get("i18n")
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

	fromRaw := strings.TrimSpace(ctx.Query("from"))
	toRaw := strings.TrimSpace(ctx.Query("to"))
	if fromRaw == "" && toRaw == "" {
		sendTodayHistory(ctx, userID, localizer)
		return
	}
	if fromRaw == "" || toRaw == "" {
		helper.SendError(ctx, http.StatusBadRequest, "from e to devem ser informados juntos")
		return
	}

	from, err := time.Parse(historyDateLayout, fromRaw)
	if err != nil {
		helper.SendError(ctx, http.StatusBadRequest, "from inválido: use YYYY-MM-DD")
		return
	}
	to, err := time.Parse(historyDateLayout, toRaw)
	if err != nil {
		helper.SendError(ctx, http.StatusBadRequest, "to inválido: use YYYY-MM-DD")
		return
	}
	if to.Before(from) {
		helper.SendError(ctx, http.StatusBadRequest, "to deve ser igual ou posterior a from")
		return
	}
	if to.Sub(from) > 366*24*time.Hour {
		helper.SendError(ctx, http.StatusBadRequest, "intervalo máximo de 366 dias")
		return
	}

	histories := make([]config.History, 0)
	if err := db.
		Where("user_id = ? AND event_date >= ? AND event_date <= ?", userID, from.Format(historyDateLayout), to.Format(historyDateLayout)).
		Order("event_date asc").
		Find(&histories).Error; err != nil {
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, localizer)
		return
	}

	responses := make([]HistoryResponse, 0, len(histories))
	for i := range histories {
		responses = append(responses, ConvertHistoryToHistoryResponse(&histories[i]))
	}
	helper.SendSuccess(ctx, responses)
}

func sendTodayHistory(ctx *gin.Context, userID string, localizer *i18n.Localizer) {
	history := config.History{}
	today := time.Now().Format(historyDateLayout)
	if err := db.Where("user_id = ? AND event_date = ?", userID, today).First(&history).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			helper.SendErrorDefault(ctx, http.StatusNotFound, localizer)
			return
		}
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, localizer)
		return
	}

	helper.SendSuccess(ctx, ConvertHistoryToHistoryResponse(&history))
}
