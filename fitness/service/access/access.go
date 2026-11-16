package access

import (
	"net/http"
	"strings"

	"health/helper"
	"health/integration/users"
	"health/schemas/link"
	"health/service/links"
)

// Denied describes a refused operation. Handlers translate Status into the
// localized default error body.
type Denied struct {
	Status int
}

func targetOf(caller helper.Caller, requested string) string {
	requested = strings.TrimSpace(requested)
	if requested == "" {
		return caller.ID
	}
	return requested
}

// ReadTarget resolves the student whose plan the caller may read.
func ReadTarget(caller helper.Caller, requested string) (string, *Denied) {
	studentID := targetOf(caller, requested)
	if studentID == caller.ID {
		return studentID, nil
	}

	if !caller.IsProfessional() {
		return "", &Denied{Status: http.StatusNotFound}
	}

	if _, denied := eligibleStudent(studentID); denied != nil {
		return "", denied
	}

	relation, ok := links.ActiveAny(studentID, caller.ID)
	if !ok {
		return "", &Denied{Status: http.StatusNotFound}
	}
	if !caller.HasRole(relation.Domain.Role()) {
		return "", &Denied{Status: http.StatusForbidden}
	}

	return studentID, nil
}

// WriteTarget resolves the student whose plan the caller may change.
func WriteTarget(caller helper.Caller, requested string, domain link.Domain) (string, *Denied) {
	studentID := targetOf(caller, requested)

	if studentID == caller.ID {
		relation, active := links.ActiveInDomain(studentID, domain)
		if !active {
			return studentID, nil
		}
		// The link only blocks the student while the professional still holds
		// the role of the domain.
		profile, found, err := users.Get(relation.ProfessionalID)
		if err != nil {
			return "", &Denied{Status: http.StatusServiceUnavailable}
		}
		if !found || !profile.Active() || !profile.HasRole(domain.Role()) {
			_ = links.EndActive(relation.ID, "SYSTEM")
			return studentID, nil
		}
		return "", &Denied{Status: http.StatusForbidden}
	}

	if !caller.IsProfessional() {
		return "", &Denied{Status: http.StatusNotFound}
	}
	if !caller.HasRole(domain.Role()) {
		return "", &Denied{Status: http.StatusForbidden}
	}

	if _, denied := eligibleStudent(studentID); denied != nil {
		return "", denied
	}

	if _, ok := links.ActiveForProfessional(studentID, caller.ID, domain); !ok {
		return "", &Denied{Status: http.StatusNotFound}
	}

	return studentID, nil
}

// StudentProfile validates the target still has an active account with the
// student role. Links are revoked when the account stops being eligible.
func StudentProfile(studentID string) (users.Profile, *Denied) {
	return eligibleStudent(studentID)
}

func eligibleStudent(studentID string) (users.Profile, *Denied) {
	profile, found, err := users.Get(studentID)
	if err != nil {
		return users.Profile{}, &Denied{Status: http.StatusServiceUnavailable}
	}
	if !found || !users.StudentEligible(profile) {
		_ = links.EndAllActiveForStudent(studentID, "SYSTEM")
		return users.Profile{}, &Denied{Status: http.StatusNotFound}
	}
	return profile, nil
}
