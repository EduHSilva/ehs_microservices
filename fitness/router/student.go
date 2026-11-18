package router

import (
	"health/handler/health/student"

	"github.com/gin-gonic/gin"
)

func initStudentRoutes(api *gin.Engine) {
	student.InitHandler()

	api.GET("students", student.ListStudentsHandler)
	api.POST("students", student.InviteStudentHandler)
	api.GET("students/:id", student.GetStudentHandler)

	api.GET("students/links", student.ListMyLinksHandler)
	api.GET("students/invites", student.ListInvitesHandler)
	api.POST("students/invites/accept", student.AcceptInviteHandler)
	api.POST("students/invites/decline", student.DeclineInviteHandler)
	api.POST("students/invites/:id/resend", student.ResendInviteHandler)
	api.DELETE("students/invites/:id", student.RevokeInviteHandler)

	api.PATCH("students/links/:id", student.UpdateLinkHandler)
	api.DELETE("students/links/:id", student.EndLinkHandler)
}
