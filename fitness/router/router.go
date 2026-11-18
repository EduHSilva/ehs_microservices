package router

import (
	"health/config"
	_ "health/docs"
	"net/http"
	"os"

	"github.com/gin-contrib/cors"
	"github.com/gin-gonic/gin"
	"github.com/nicksnyder/go-i18n/v2/i18n"
	swaggerFiles "github.com/swaggo/files"
	ginSwagger "github.com/swaggo/gin-swagger"
)

func Init() {
	router := gin.Default()

	router.Use(cors.New(cors.Config{
		AllowOrigins: []string{"*"},
		AllowMethods: []string{"GET", "POST", "PUT", "PATCH", "DELETE"},
		AllowHeaders: []string{"Content-Type", "Authorization", "Accept-Language"},
	}))
	router.Use(localization)

	initRoutes(router)

	port := os.Getenv("PORT")
	if port == "" {
		port = os.Getenv("SERVICE_PORT")
	}
	if port == "" {
		port = "8080"
	}

	err := router.Run(":" + port)
	if err != nil {
		return

	}
}

func localization(ctx *gin.Context) {
	locale := ctx.GetHeader("Accept-Language")
	if locale == "" {
		locale = "en"
	}

	ctx.Set("i18n", i18n.NewLocalizer(config.GetBundler(), locale))
	ctx.Set("locale", locale)
	ctx.Next()
}

func initRoutes(router *gin.Engine) {
	router.GET("/actuator/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{"status": "UP"})
	})

	initWorkoutRoutes(router)
	initDietRoutes(router)
	initConfigRoutes(router)
	initStudentRoutes(router)

	router.GET("/swagger/*any", ginSwagger.WrapHandler(swaggerFiles.Handler))
}
