package com.edu.silva.study.infra.config

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.hibernate.Session
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException
import java.util.UUID

@Component
class HibernateUserFilter : OncePerRequestFilter() {
    @PersistenceContext
    private lateinit var entityManager: EntityManager

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return path.startsWith("/actuator/") || path.startsWith("/swagger-ui") ||
            path.startsWith("/v3/api-docs") || path.startsWith("/ping")
    }

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val header = request.getHeader("X-User-ID")
        val userId = try {
            header?.let(UUID::fromString)
        } catch (_: IllegalArgumentException) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid X-User-ID")
            return
        }

        if (userId == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "X-User-ID is required")
            return
        }

        try {
            UserContext.setUser(userId)
            entityManager.unwrap(Session::class.java)
                .enableFilter("userFilter")
                .setParameter("userId", userId)
            filterChain.doFilter(request, response)
        } finally {
            UserContext.clear()
        }
    }
}
