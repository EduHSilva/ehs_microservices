package student

import (
	"fmt"
	"strings"

	"health/helper"
)

type InviteStudentRequest struct {
	Email  string `json:"email"`
	Domain string `json:"domain"`
	Goal   string `json:"goal"`
}

func (r InviteStudentRequest) Validate() error {
	if strings.TrimSpace(r.Email) == "" {
		return helper.ErrParamIsRequired("email", "request body")
	}
	if strings.TrimSpace(r.Domain) == "" {
		return helper.ErrParamIsRequired("domain", "request body")
	}
	if len(strings.TrimSpace(r.Goal)) > 255 {
		return fmt.Errorf("param: goal (type: request body) must have at most 255 characters")
	}
	return nil
}

type InviteTokenRequest struct {
	Token string `json:"token"`
}

func (r InviteTokenRequest) Validate() error {
	if strings.TrimSpace(r.Token) == "" {
		return helper.ErrParamIsRequired("token", "request body")
	}
	return nil
}

type UpdateLinkRequest struct {
	Goal *string `json:"goal"`
}

func (r UpdateLinkRequest) Validate() error {
	if r.Goal == nil {
		return helper.ErrParamIsRequired("goal", "request body")
	}
	if len(strings.TrimSpace(*r.Goal)) > 255 {
		return fmt.Errorf("param: goal (type: request body) must have at most 255 characters")
	}
	return nil
}
