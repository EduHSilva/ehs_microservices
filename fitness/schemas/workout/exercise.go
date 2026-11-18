package workout

import "gorm.io/gorm"

type Exercise struct {
	gorm.Model
	Name           string `json:"name"`
	NamePt         string `json:"name_pt"`
	BodyPart       string `json:"body_part"`
	Instructions   string `json:"instructions" gorm:"size:1000"`
	InstructionsPt string `json:"instructions_pt" gorm:"size:1000"`
	ExternalID     string `json:"external_id" gorm:"unique"`
	Target         string `json:"target"`
	ImageURL       string `json:"image_url" gorm:"size:1000"`
	AuthorID       string `json:"author_id" gorm:"index"`
}
