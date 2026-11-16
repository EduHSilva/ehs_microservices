package student

import (
	"crypto/rand"
	"encoding/hex"
	"net/http"
	"os"
	"strconv"
	"strings"
	"time"

	"health/helper"
	"health/integration/notifications"
	"health/integration/users"
	"health/schemas/link"
	"health/service/links"

	"github.com/gin-gonic/gin"
	"github.com/nicksnyder/go-i18n/v2/i18n"
)

const defaultInviteTTLHours = 7 * 24

const (
	endedByProfessional = "PROFESSIONAL"
	endedByStudent      = "STUDENT"
)

func localizerOf(ctx *gin.Context) *i18n.Localizer {
	getI18n, _ := ctx.Get("i18n")
	return getI18n.(*i18n.Localizer)
}

func sendLocalizedError(ctx *gin.Context, status int, messageID string) {
	message := localizerOf(ctx).MustLocalize(&i18n.LocalizeConfig{MessageID: messageID})
	helper.SendError(ctx, status, message)
}

func inviteTTL() time.Duration {
	hours, err := strconv.Atoi(strings.TrimSpace(os.Getenv("FITNESS_INVITE_TTL_HOURS")))
	if err != nil || hours <= 0 {
		hours = defaultInviteTTLHours
	}
	return time.Duration(hours) * time.Hour
}

func newToken() (string, error) {
	buffer := make([]byte, 24)
	if _, err := rand.Read(buffer); err != nil {
		return "", err
	}
	return hex.EncodeToString(buffer), nil
}

func normalizeEmail(email string) string {
	return strings.ToLower(strings.TrimSpace(email))
}

func inviteURL(token string) string {
	base := strings.TrimRight(os.Getenv("SERVER_URL"), "/")
	path := os.Getenv("FITNESS_INVITE_PATH")
	if path == "" {
		path = "/app/fitness/?invite="
	}
	return base + path + token
}

// sendInviteEmail notifies the invited e-mail. A broker failure is logged and
// does not roll back the invitation, which the professional can resend.
func sendInviteEmail(invite link.StudentProfessional, professionalName string) {
	username := invite.StudentName
	if username == "" {
		username = invite.InvitedEmail
	}

	message := notifications.EmailMessage{
		UserID:    stringOrEmpty(invite.StudentID),
		Username:  username,
		EmailTo:   invite.InvitedEmail,
		LocaleTag: "pt-BR",
		EmailType: "FITNESS_INVITE",
		BtnURL:    inviteURL(invite.Token),
		Variables: map[string]any{
			"professional": professionalName,
			"domain":       string(invite.Domain),
		},
	}

	if err := notifications.Send(message); err != nil {
		logger.ErrF("fitness invite email to %s failed: %v", invite.InvitedEmail, err)
	}
}

func stringOrEmpty(value *string) string {
	if value == nil {
		return ""
	}
	return *value
}

func parseUintParam(value string) uint {
	parsed, err := strconv.ParseUint(strings.TrimSpace(value), 10, 64)
	if err != nil {
		return 0
	}
	return uint(parsed)
}

// profileOf enriches a card with the current account data, falling back to what
// was stored in the link when the users service is unavailable.
func profileOf(relation link.StudentProfessional) users.Profile {
	if relation.StudentID == nil || !users.Enabled() {
		return users.Profile{}
	}
	profile, found, err := users.Get(*relation.StudentID)
	if err != nil || !found {
		return users.Profile{}
	}
	return profile
}

// callerProfile loads the authenticated account from the users service.
func callerProfile(caller helper.Caller) (users.Profile, *int) {
	profile, found, err := users.Get(caller.ID)
	if err != nil {
		status := http.StatusServiceUnavailable
		return users.Profile{}, &status
	}
	if !found {
		status := http.StatusUnauthorized
		return users.Profile{}, &status
	}
	return profile, nil
}

// professionalDomain validates that the caller can manage the domain: an
// unknown domain is a bad request, a missing role is forbidden.
func professionalDomain(caller helper.Caller, rawDomain string) (link.Domain, *int) {
	domain, ok := link.ParseDomain(strings.ToUpper(strings.TrimSpace(rawDomain)))
	if !ok {
		status := http.StatusBadRequest
		return "", &status
	}
	if !caller.HasRole(domain.Role()) {
		status := http.StatusForbidden
		return "", &status
	}
	return domain, nil
}

// ownedLink loads an invitation/link and hides rows that belong to someone
// else, answering 404 to avoid revealing other people's links.
func ownedLink(id uint, caller helper.Caller) (link.StudentProfessional, bool) {
	relation, found := links.Find(id)
	if !found {
		return link.StudentProfessional{}, false
	}
	if relation.ProfessionalID == caller.ID {
		return relation, true
	}
	if relation.StudentID != nil && *relation.StudentID == caller.ID {
		return relation, true
	}
	return link.StudentProfessional{}, false
}
