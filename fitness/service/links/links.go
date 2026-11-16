package links

import (
	"time"

	"health/schemas/link"

	"gorm.io/gorm"
)

var db *gorm.DB

func Init(database *gorm.DB) {
	db = database
}

func ActiveForProfessional(studentID, professionalID string, domain link.Domain) (link.StudentProfessional, bool) {
	var relation link.StudentProfessional
	err := db.
		Where("student_id = ? AND professional_id = ? AND domain = ? AND status = ?",
			studentID, professionalID, domain, link.StatusActive).
		First(&relation).Error
	return relation, err == nil
}

// ActiveAny returns an active link between the professional and the student in
// either domain, used for cross-domain reads.
func ActiveAny(studentID, professionalID string) (link.StudentProfessional, bool) {
	var relation link.StudentProfessional
	err := db.
		Where("student_id = ? AND professional_id = ? AND status = ?",
			studentID, professionalID, link.StatusActive).
		First(&relation).Error
	return relation, err == nil
}

func StudentHasActiveDomain(studentID string, domain link.Domain) bool {
	var count int64
	db.Model(&link.StudentProfessional{}).
		Where("student_id = ? AND domain = ? AND status = ?", studentID, domain, link.StatusActive).
		Count(&count)
	return count > 0
}

// ActiveInDomain returns the active link of a student in a domain, whatever
// professional holds it, so the caller can revalidate the professional side.
func ActiveInDomain(studentID string, domain link.Domain) (link.StudentProfessional, bool) {
	var relation link.StudentProfessional
	err := db.
		Where("student_id = ? AND domain = ? AND status = ?", studentID, domain, link.StatusActive).
		First(&relation).Error
	return relation, err == nil
}

func ActiveForStudent(studentID string) []link.StudentProfessional {
	var relations []link.StudentProfessional
	db.Where("student_id = ? AND status = ?", studentID, link.StatusActive).
		Order("domain").
		Find(&relations)
	return relations
}

// PendingForProfessional lists the invitations the professional sent and that
// the student has not answered yet.
func PendingForProfessional(professionalID string) ([]link.StudentProfessional, error) {
	relations := make([]link.StudentProfessional, 0)
	err := db.
		Where("professional_id = ? AND status = ?", professionalID, link.StatusPending).
		Order("created_at DESC").
		Find(&relations).Error
	return relations, err
}

// PendingByEmail lists the invitations addressed to an e-mail, used by the
// student to see who wants to accompany them.
func PendingByEmail(email string) ([]link.StudentProfessional, error) {
	relations := make([]link.StudentProfessional, 0)
	err := db.
		Where("invited_email = ? AND status = ?", email, link.StatusPending).
		Order("created_at DESC").
		Find(&relations).Error
	return relations, err
}

// PendingFor finds a pending invitation already sent by the professional to
// the e-mail in the same domain, to avoid duplicates.
func PendingFor(professionalID, email string, domain link.Domain) (link.StudentProfessional, bool) {
	var relation link.StudentProfessional
	err := db.
		Where("professional_id = ? AND invited_email = ? AND domain = ? AND status = ?",
			professionalID, email, domain, link.StatusPending).
		First(&relation).Error
	return relation, err == nil
}

func ActiveForProfessionalList(professionalID string, domain link.Domain) ([]link.StudentProfessional, error) {
	var relations []link.StudentProfessional
	query := db.Where("professional_id = ? AND status = ?", professionalID, link.StatusActive)
	if domain.Valid() {
		query = query.Where("domain = ?", domain)
	}
	err := query.Order("domain, student_name").Find(&relations).Error
	return relations, err
}

func FindActive(id uint) (link.StudentProfessional, bool) {
	var relation link.StudentProfessional
	err := db.Where("id = ? AND status = ?", id, link.StatusActive).First(&relation).Error
	return relation, err == nil
}

func FindByToken(token string) (link.StudentProfessional, bool) {
	var relation link.StudentProfessional
	err := db.Where("token = ?", token).First(&relation).Error
	return relation, err == nil
}

func Find(id uint) (link.StudentProfessional, bool) {
	var relation link.StudentProfessional
	err := db.First(&relation, id).Error
	return relation, err == nil
}

func EndActive(id uint, by string) error {
	now := time.Now()
	return db.Model(&link.StudentProfessional{}).
		Where("id = ? AND status = ?", id, link.StatusActive).
		Updates(map[string]interface{}{
			"status":   link.StatusEnded,
			"ended_at": now,
			"ended_by": by,
		}).Error
}

// EndAllActiveForStudent revokes every active link when the student stops being
// eligible to use the product.
func EndAllActiveForStudent(studentID, by string) error {
	now := time.Now()
	return db.Model(&link.StudentProfessional{}).
		Where("student_id = ? AND status = ?", studentID, link.StatusActive).
		Updates(map[string]interface{}{
			"status":   link.StatusEnded,
			"ended_at": now,
			"ended_by": by,
		}).Error
}

// ExpireDue marks pending invitations past their deadline as EXPIRED.
func ExpireDue(now time.Time) error {
	return db.Model(&link.StudentProfessional{}).
		Where("status = ? AND expires_at < ?", link.StatusPending, now).
		Update("status", link.StatusExpired).Error
}
