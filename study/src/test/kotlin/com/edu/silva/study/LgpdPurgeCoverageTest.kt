package com.edu.silva.study

import com.edu.silva.common.sql.LgpdCoverageVerifier
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import javax.sql.DataSource

@SpringBootTest
@ActiveProfiles("test")
class LgpdPurgeCoverageTest {

    @Autowired
    private lateinit var dataSource: DataSource

    @Test
    fun `function covers schema`() {
        val problems = LgpdCoverageVerifier.verify(dataSource, "db/lgpd_purge.sql", emptySet())
        assertTrue(problems.isEmpty()) { problems.joinToString("\n") }
    }
}
