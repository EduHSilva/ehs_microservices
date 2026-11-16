package helper

import (
	"fmt"
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/nicksnyder/go-i18n/v2/i18n"
)

func ErrParamIsRequired(name, typ string) error {
	return fmt.Errorf("param: %s (type: %s) is required", name, typ)
}

func SendErrorDefault(ctx *gin.Context, code int, getI18n *i18n.Localizer) {
	message := ""
	switch code {
	case http.StatusNotFound:
		message = getI18n.MustLocalize(&i18n.LocalizeConfig{
			MessageID: "notFound",
		})
		break
	case http.StatusInternalServerError:
		message = getI18n.MustLocalize(&i18n.LocalizeConfig{
			MessageID: "genericError500",
		})
	case http.StatusUnauthorized:
		message = getI18n.MustLocalize(&i18n.LocalizeConfig{
			MessageID: "unauthorized",
		})
	case http.StatusForbidden:
		message = getI18n.MustLocalize(&i18n.LocalizeConfig{
			MessageID: "forbidden",
		})
	case http.StatusServiceUnavailable:
		message = getI18n.MustLocalize(&i18n.LocalizeConfig{
			MessageID: "unavailable",
		})

	}

	ctx.Header("Content-Type", "application/json; charset=utf-8")
	ctx.JSON(code, gin.H{
		"code":    code,
		"message": message,
	})
}

func SendError(ctx *gin.Context, code int, msg string) {
	ctx.Header("Content-Type", "application/json; charset=utf-8")
	ctx.JSON(code, gin.H{
		"code":    code,
		"message": msg,
	})
}

func SendSuccess(ctx *gin.Context, data interface{}) {
	ctx.Header("Content-Type", "application/json; charset=utf-8")

	ctx.JSON(http.StatusOK, gin.H{
		"message": "ok",
		"data":    data,
	})
}

type ErrorResponse struct {
	Message string `json:"message"`
	Code    string `json:"code"`
}
