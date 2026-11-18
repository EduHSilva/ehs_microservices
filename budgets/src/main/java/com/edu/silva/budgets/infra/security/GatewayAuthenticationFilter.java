package com.edu.silva.budgets.infra.security;

import com.edu.silva.budgets.infra.config.UserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * O gateway valida o JWT e injeta {@code X-User-ID}/{@code X-User-Roles};
 * este filtro converte esses headers em autenticação do Spring Security.
 */
@Component
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    private static final String USER_ID_HEADER = "X-User-ID";
    private static final String ROLES_HEADER = "X-User-Roles";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            if (!authenticate(request, response)) {
                return;
            }
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }

    private boolean authenticate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String userIdHeader = request.getHeader(USER_ID_HEADER);
        if (userIdHeader == null) {
            return true;
        }

        UUID userId;
        try {
            userId = UUID.fromString(userIdHeader);
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid X-User-ID");
            return false;
        }

        UserContext.setUser(userId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        authorities(request.getHeader(ROLES_HEADER))
                )
        );
        return true;
    }

    private List<GrantedAuthority> authorities(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) {
            return List.of();
        }
        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }
}
