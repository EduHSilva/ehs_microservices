package main

import (
	"health/config"
	"health/eureka"
	"health/router"
	"health/service/links"
	"time"
)

var (
	logger *config.Logger
)

// @title Fitness API
// @version 1.0
// @description Treinos, dieta, agenda e vínculo aluno/profissional do Fitness.
// @description
// @description As rotas são expostas pelo gateway em /fitness/**. O gateway valida o JWT
// @description e injeta X-User-ID e X-User-Roles; o serviço valida o vínculo e o domínio.
// @description O aluno autorizado ou o profissional do domínio lê/escreve o plano de um
// @description aluno passando student_id (ausente = o próprio usuário do token).
// @host localhost
// @BasePath /
// @securityDefinitions.apikey BearerAuth
// @in header
// @name Authorization
func main() {
	logger = config.GetLogger("main")
	err := config.Init()
	if err != nil {
		logger.ErrF("Configuration initialization failed with error: %v", err)
		return
	}

	eureka.Register()
	eureka.StartHeartbeat()
	expireInvites()
	router.Init()
}

func expireInvites() {
	go func() {
		for {
			if err := links.ExpireDue(time.Now()); err != nil {
				logger.ErrF("expire fitness invites failed: %v", err)
			}
			time.Sleep(time.Hour)
		}
	}()
}
