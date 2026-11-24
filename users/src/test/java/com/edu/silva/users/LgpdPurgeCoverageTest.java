package com.edu.silva.users;

import com.edu.silva.common.sql.LgpdCoverageVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class LgpdPurgeCoverageTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void functionCoversSchema() throws Exception {
        // user_roles é descoberta pela função via FK para users; aqui ela é declarada à parte.
        List<String> problems = LgpdCoverageVerifier.verify(
                dataSource, "db/lgpd_purge.sql", Set.of("user_roles"));
        assertTrue(problems.isEmpty(), () -> String.join("\n", problems));
    }
}
