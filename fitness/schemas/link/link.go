package link

import (
	"time"

	"gorm.io/gorm"
)

// Domain is the professional area that owns a link. A student can have one
// active professional per domain.
type Domain string

const (
	DomainPersonal     Domain = "PERSONAL"
	DomainNutritionist Domain = "NUTRITIONIST"
)

func ParseDomain(value string) (Domain, bool) {
	switch Domain(value) {
	case DomainPersonal:
		return DomainPersonal, true
	case DomainNutritionist:
		return DomainNutritionist, true
	default:
		return "", false
	}
}

func (d Domain) Role() string {
	if d == DomainPersonal {
		return "HEALTH_PERSONAL"
	}
	return "HEALTH_NUTRITIONIST"
}

func (d Domain) Valid() bool {
	return d == DomainPersonal || d == DomainNutritionist
}

type Status string

const (
	StatusPending  Status = "PENDING"
	StatusActive   Status = "ACTIVE"
	StatusDeclined Status = "DECLINED"
	StatusRevoked  Status = "REVOKED"
	StatusEnded    Status = "ENDED"
	StatusExpired  Status = "EXPIRED"
)

// StudentProfessional is both the invitation and the resulting link. A row is
// created as PENDING when the professional invites the student by e-mail and
// becomes ACTIVE after the student accepts. StudentID stays empty while the
// invited e-mail has no account yet.
type StudentProfessional struct {
	gorm.Model

	StudentID      *string `gorm:"type:uuid;index;uniqueIndex:idx_links_student_domain_active,where:status = 'ACTIVE'" json:"student_id"`
	ProfessionalID string  `gorm:"type:uuid;not null;index" json:"professional_id"`
	Domain         Domain  `gorm:"type:varchar(20);not null;index;uniqueIndex:idx_links_student_domain_active,where:status = 'ACTIVE'" json:"domain"`
	Goal           string  `gorm:"size:255" json:"goal"`

	Status       Status     `gorm:"type:varchar(20);not null;index" json:"status"`
	InvitedEmail string     `gorm:"size:255;not null;index" json:"invited_email"`
	StudentName  string     `gorm:"size:255" json:"student_name"`
	Token        string     `gorm:"size:64;not null;uniqueIndex" json:"-"`
	ExpiresAt    time.Time  `gorm:"not null" json:"expires_at"`
	AcceptedAt   *time.Time `json:"accepted_at"`
	EndedAt      *time.Time `json:"ended_at"`
	EndedBy      string     `gorm:"size:20" json:"ended_by"`
}

func (l *StudentProfessional) Expired(now time.Time) bool {
	return l.Status == StatusPending && now.After(l.ExpiresAt)
}
