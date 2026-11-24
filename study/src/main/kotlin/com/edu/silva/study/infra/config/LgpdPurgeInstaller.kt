package com.edu.silva.study.infra.config

import com.edu.silva.common.sql.SqlResourceExecutor
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Component
@Profile("!test")
class LgpdPurgeInstaller(private val dataSource: DataSource) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(LgpdPurgeInstaller::class.java)

    override fun run(args: ApplicationArguments) {
        val installed = SqlResourceExecutor.executeIfPostgres(dataSource, "db/lgpd_purge.sql")
        if (installed) {
            log.info("Função lgpd_purge_user disponível neste banco")
        } else {
            log.warn("Banco não é PostgreSQL; função lgpd_purge_user não instalada")
        }
    }
}
