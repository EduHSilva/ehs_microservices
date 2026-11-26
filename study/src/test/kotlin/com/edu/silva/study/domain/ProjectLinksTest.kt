package com.edu.silva.study.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ProjectLinksTest {

    @Test
    fun `vazio vira nulo`() {
        assertNull(ProjectLinks.normalize(null))
        assertNull(ProjectLinks.normalize(""))
        assertNull(ProjectLinks.normalize("   "))
    }

    @Test
    fun `link sem esquema recebe https`() {
        assertEquals("https://udemy.com/course/estatistica", ProjectLinks.normalize("udemy.com/course/estatistica"))
    }

    @Test
    fun `link com esquema e mantido e aparado`() {
        assertEquals("https://udemy.com/course/x", ProjectLinks.normalize("  https://udemy.com/course/x  "))
        assertEquals("http://exemplo.com", ProjectLinks.normalize("http://exemplo.com"))
    }

    @Test
    fun `esquema em maiusculas nao e duplicado`() {
        assertEquals("HTTPS://exemplo.com", ProjectLinks.normalize("HTTPS://exemplo.com"))
    }
}
