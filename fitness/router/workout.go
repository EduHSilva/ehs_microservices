package router

import (
	"health/handler/health/workout"

	"github.com/gin-gonic/gin"
)

func initWorkoutRoutes(api *gin.Engine) {
	workout.InitHandler()

	api.GET("workouts", workout.GetWorkoutsHandler)
	api.GET("workout", workout.GetWorkoutHandler)
	api.POST("workout", workout.CreateWorkoutHandler)
	api.DELETE("workout", workout.DeleteWorkoutHandler)
	api.PUT("workout", workout.UpdateWorkoutHandler)
	api.GET("workouts/week", workout.GetWorkoutWeekHandler)
	api.PUT("workouts/week", workout.UpdateWorkoutWeekHandler)
	api.GET("workout/exercises", workout.GetExercisesHandler)
	api.POST("workout/exercises", workout.CreateExerciseHandler)
}
