package com.edu.silva.study.infra.config

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException

@Component
class TimeZoneFilter : OncePerRequestFilter() {

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
        RequestTimeZone.set(request.getHeader("X-Time-Zone"))
        try {
            filterChain.doFilter(request, response)
        } finally {
            RequestTimeZone.clear()
        }
    }
}
