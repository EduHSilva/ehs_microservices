package com.edu.silva.users.infra.security;

import com.edu.silva.users.domain.entities.User;
import com.edu.silva.users.domain.enums.UserStatus;
import com.edu.silva.users.repositories.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UserRepository userRepository;

    public SecurityFilter(TokenService tokenService, UserRepository userRepository) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }


    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        String token = this.recoverToken(request);
        if (token != null) {
            var decoded = tokenService.verify(token);
            if (decoded == null) {
                filterChain.doFilter(request, response);
                return;
            }
            Optional<User> user = userRepository.findById(UUID.fromString(decoded.getSubject()));
            if (user.isEmpty()) return;
            if (user.get().getStatus() != UserStatus.OK) {
                filterChain.doFilter(request, response);
                return;
            }
            if (user.get().getTokenVersion() != tokenService.tokenVersion(decoded)) {
                filterChain.doFilter(request, response);
                return;
            }
            UserDetails userDetails = user.get();
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        return authHeader.replace("Bearer ", "");
    }
}
