package diet

import (
	"net/http"
	"strings"

	"health/helper"
	"health/schemas/diet"

	"github.com/gin-gonic/gin"
	"github.com/nicksnyder/go-i18n/v2/i18n"
)

const (
	foodSearchLimit     = 30
	foodSearchMaxTokens = 8

	accentedRunes = "áàâãäÁÀÂÃÄéèêëÉÈÊËíìîïÍÌÎÏóòôõöÓÒÔÕÖúùûüÚÙÛÜçñÇÑ"
	plainRunes    = "aaaaaAAAAAeeeeEEEEiiiiIIIIoooooOOOOOuuuuUUUUcnCN"
)

// foodNameNormalized remove acentos e caixa no próprio Postgres, sem depender de
// extensões (unaccent). Cobre os caracteres do pt-BR usados no catálogo.
var foodNameNormalized = "lower(translate(name, '" + accentedRunes + "', '" + plainRunes + "'))"

var accentFolder = strings.NewReplacer(
	"á", "a", "à", "a", "â", "a", "ã", "a", "ä", "a",
	"é", "e", "è", "e", "ê", "e", "ë", "e",
	"í", "i", "ì", "i", "î", "i", "ï", "i",
	"ó", "o", "ò", "o", "ô", "o", "õ", "o", "ö", "o",
	"ú", "u", "ù", "u", "û", "u", "ü", "u",
	"ç", "c", "ñ", "n",
)

// SearchFoodHandler busca no catálogo global de alimentos.
// @Summary Busca alimentos
// @Description Busca por palavras do nome, ignorando acentos e caixa.
// @Tags Dieta
// @Produce json
// @Param query query string false "Trecho do nome"
// @Success 200 {object} map[string]interface{}
// @Failure 401 {object} helper.ErrorResponse
// @Router /diet/meal/food [get]
func SearchFoodHandler(ctx *gin.Context) {
	foods := make([]diet.Food, 0)
	foodsResponses := make([]FoodResponse, 0)

	getI18n, _ := ctx.Get("i18n")
	localizer := getI18n.(*i18n.Localizer)

	if _, exists := helper.GatewayCaller(ctx); !exists {
		helper.SendErrorDefault(ctx, http.StatusUnauthorized, localizer)
		return
	}

	query := db.Model(&diet.Food{})
	for _, token := range foodSearchTokens(ctx.Query("query")) {
		query = query.Where(foodNameNormalized+" LIKE ?", "%"+token+"%")
	}

	if err := query.Order("name ASC").Limit(foodSearchLimit).Find(&foods).Error; err != nil {
		helper.SendErrorDefault(ctx, http.StatusInternalServerError, localizer)
		return
	}

	for _, food := range foods {
		response := ConvertFoodsToResponse(&food)
		foodsResponses = append(foodsResponses, response)
	}

	helper.SendSuccess(ctx, foodsResponses)
}

// foodSearchTokens quebra a busca em palavras normalizadas ("Açúcar, cristal"
// vira ["acucar", "cristal"]) para combinar com AND, sem exigir o trecho exato.
func foodSearchTokens(value string) []string {
	value = accentFolder.Replace(strings.ToLower(strings.TrimSpace(value)))
	cleaned := strings.Map(func(r rune) rune {
		if (r >= 'a' && r <= 'z') || (r >= '0' && r <= '9') {
			return r
		}
		return ' '
	}, value)

	seen := make(map[string]struct{}, foodSearchMaxTokens)
	tokens := make([]string, 0, foodSearchMaxTokens)
	for _, token := range strings.Fields(cleaned) {
		if len(token) < 2 {
			continue
		}
		if _, ok := seen[token]; ok {
			continue
		}
		seen[token] = struct{}{}
		tokens = append(tokens, token)
		if len(tokens) == foodSearchMaxTokens {
			break
		}
	}
	return tokens
}
