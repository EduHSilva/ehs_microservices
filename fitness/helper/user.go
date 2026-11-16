package helper

import (
	"strings"

	"github.com/gin-gonic/gin"
)

const (
	UserIDHeader    = "X-User-ID"
	UserRolesHeader = "X-User-Roles"

	RoleStudent      = "HEALTH_USER"
	RolePersonal     = "HEALTH_PERSONAL"
	RoleNutritionist = "HEALTH_NUTRITIONIST"
)

// Caller is the authenticated user forwarded by the gateway.
type Caller struct {
	ID    string
	Roles map[string]bool
}

func GatewayCaller(ctx *gin.Context) (Caller, bool) {
	userID := strings.TrimSpace(ctx.GetHeader(UserIDHeader))
	if userID == "" {
		return Caller{}, false
	}

	caller := Caller{ID: userID, Roles: make(map[string]bool)}
	for _, role := range strings.Split(ctx.GetHeader(UserRolesHeader), ",") {
		role = strings.TrimSpace(role)
		if role != "" {
			caller.Roles[role] = true
		}
	}

	return caller, true
}

// GatewayUserID returns the authenticated user's ID forwarded by the gateway.
func GatewayUserID(ctx *gin.Context) (string, bool) {
	userID := strings.TrimSpace(ctx.GetHeader(UserIDHeader))
	return userID, userID != ""
}

func (c Caller) HasRole(role string) bool {
	return c.Roles[role]
}

func (c Caller) IsStudent() bool {
	return c.HasRole(RoleStudent)
}

func (c Caller) IsPersonal() bool {
	return c.HasRole(RolePersonal)
}

func (c Caller) IsNutritionist() bool {
	return c.HasRole(RoleNutritionist)
}

func (c Caller) IsProfessional() bool {
	return c.IsPersonal() || c.IsNutritionist()
}

// RequestedStudent returns the target student of a request: the student_id
// query parameter wins, then the body field, and an empty value means the
// caller's own plan.
func RequestedStudent(ctx *gin.Context, bodyStudentID string) string {
	if studentID := strings.TrimSpace(ctx.Query("student_id")); studentID != "" {
		return studentID
	}
	return strings.TrimSpace(bodyStudentID)
}
