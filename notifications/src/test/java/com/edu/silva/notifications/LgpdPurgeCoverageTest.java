package com.edu.silva.notifications;

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
        List<String> problems = LgpdCoverageVerifier.verify(dataSource, "db/lgpd_purge.sql", Set.of());
        assertTrue(problems.isEmpty(), () -> String.join("\n", problems));
    }
}
