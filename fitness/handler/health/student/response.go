package student

import (
	"time"

	"health/integration/users"
	"health/schemas/link"
)

type StudentCard struct {
	LinkID    uint       `json:"link_id"`
	StudentID string     `json:"student_id,omitempty"`
	Name      string     `json:"name"`
	Email     string     `json:"email"`
	Goal      string     `json:"goal"`
	Domain    string     `json:"domain"`
	Since     *time.Time `json:"since,omitempty"`
}

type StudentsResponse struct {
	Students []StudentCard `json:"students"`
	Invites  []InviteCard  `json:"invites"`
}

type InviteCard struct {
	ID        uint      `json:"id"`
	Email     string    `json:"email"`
	Domain    string    `json:"domain"`
	Goal      string    `json:"goal"`
	Status    string    `json:"status"`
	ExpiresAt time.Time `json:"expires_at"`
	CreatedAt time.Time `json:"created_at"`
}

type IncomingInviteCard struct {
	ID               uint      `json:"id"`
	ProfessionalID   string    `json:"professional_id"`
	ProfessionalName string    `json:"professional_name"`
	Domain           string    `json:"domain"`
	Goal             string    `json:"goal"`
	ExpiresAt        time.Time `json:"expires_at"`
}

// MyLinkCard is an active link seen from the student's side.
type MyLinkCard struct {
	LinkID           uint       `json:"link_id"`
	Domain           string     `json:"domain"`
	ProfessionalID   string     `json:"professional_id"`
	ProfessionalName string     `json:"professional_name"`
	Goal             string     `json:"goal"`
	Since            *time.Time `json:"since,omitempty"`
}

func ConvertLinkToStudentCard(relation link.StudentProfessional, profile users.Profile) StudentCard {
	card := StudentCard{
		LinkID: relation.ID,
		Name:   relation.StudentName,
		Email:  relation.InvitedEmail,
		Goal:   relation.Goal,
		Domain: string(relation.Domain),
		Since:  relation.AcceptedAt,
	}
	if relation.StudentID != nil {
		card.StudentID = *relation.StudentID
	}
	if profile.ID != "" {
		if profile.Username != "" {
			card.Name = profile.Username
		}
		if profile.Email != "" {
			card.Email = profile.Email
		}
	}
	return card
}

func ConvertLinkToInviteCard(relation link.StudentProfessional) InviteCard {
	return InviteCard{
		ID:        relation.ID,
		Email:     relation.InvitedEmail,
		Domain:    string(relation.Domain),
		Goal:      relation.Goal,
		Status:    string(relation.Status),
		ExpiresAt: relation.ExpiresAt,
		CreatedAt: relation.CreatedAt,
	}
}

func ConvertLinkToIncomingInvite(relation link.StudentProfessional, professional users.Profile) IncomingInviteCard {
	return IncomingInviteCard{
		ID:               relation.ID,
		ProfessionalID:   relation.ProfessionalID,
		ProfessionalName: professional.Username,
		Domain:           string(relation.Domain),
		Goal:             relation.Goal,
		ExpiresAt:        relation.ExpiresAt,
	}
}

func ConvertLinkToMyLinkCard(relation link.StudentProfessional, professional users.Profile) MyLinkCard {
	return MyLinkCard{
		LinkID:           relation.ID,
		Domain:           string(relation.Domain),
		ProfessionalID:   relation.ProfessionalID,
		ProfessionalName: professional.Username,
		Goal:             relation.Goal,
		Since:            relation.AcceptedAt,
	}
}
