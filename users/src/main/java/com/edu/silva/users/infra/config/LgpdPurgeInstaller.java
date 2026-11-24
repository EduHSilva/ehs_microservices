package com.edu.silva.users.infra.config;

import com.edu.silva.common.sql.SqlResourceExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class LgpdPurgeInstaller implements ApplicationRunner {

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        boolean installed = SqlResourceExecutor.executeIfPostgres(dataSource, "db/lgpd_purge.sql");
        if (installed) {
            log.info("Função lgpd_purge_user disponível neste banco");
        } else {
            log.warn("Banco não é PostgreSQL; função lgpd_purge_user não instalada");
        }
    }
}
