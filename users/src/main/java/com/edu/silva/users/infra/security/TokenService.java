package com.edu.silva.users.infra.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.edu.silva.common.enums.AppScope;
import com.edu.silva.users.domain.entities.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class TokenService {

    public static final String PURPOSE_APP_ACCESS = "APP_ACCESS";
    public static final String PURPOSE_PASSWORD_RESET = "PASSWORD_RESET";

    private static final Duration ACTION_TOKEN_TTL = Duration.ofMinutes(30);

    @Value("${api.security.token.secret}")
    private String secret;

    public String generateToken(User user, AppScope app) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            List<String> roles = user.getRoles().stream()
                    .map(Enum::name)
                    .toList();
            return JWT.create()
                    .withIssuer("auth")
                    .withSubject(String.valueOf(user.getId()))
                    .withAudience(app.name())
                    .withExpiresAt(generateExpirationDate())
                    .withClaim("roles", roles)
                    .withClaim("ver", user.getTokenVersion())
                    .sign(algorithm);
        } catch (JWTCreationException e) {
            throw new RuntimeException("Error creating JWT token", e);
        }
    }

    public DecodedJWT verify(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT
                    .require(algorithm)
                    .withIssuer("auth")
                    .build()
                    .verify(token);
        } catch (JWTVerificationException e) {
            return null;
        }
    }

    /**
     * Token de ação enviado por e-mail (curta duração), usado no vínculo de
     * acesso a uma aplicação.
     */
    public String generateAppAccessToken(User user, AppScope app, String role) {
        return generateActionToken(user, PURPOSE_APP_ACCESS, builder -> builder
                .withClaim("app", app.name())
                .withClaim("role", role));
    }

    /**
     * Token de ação enviado por e-mail (curta duração), usado na redefinição
     * de senha.
     */
    public String generatePasswordResetToken(User user) {
        return generateActionToken(user, PURPOSE_PASSWORD_RESET, builder -> builder);
    }

    /**
     * Verifica assinatura, emissor, expiração e a finalidade ({@code purpose})
     * do token de ação.
     */
    public DecodedJWT verifyActionToken(String token, String purpose) {
        DecodedJWT jwt = verify(token);
        if (jwt == null) {
            return null;
        }
        return purpose.equals(jwt.getClaim("purpose").asString()) ? jwt : null;
    }

    private String generateActionToken(User user, String purpose,
                                       java.util.function.UnaryOperator<com.auth0.jwt.JWTCreator.Builder> customizer) {
        try {
            var builder = JWT.create()
                    .withIssuer("auth")
                    .withSubject(String.valueOf(user.getId()))
                    .withExpiresAt(Instant.now().plus(ACTION_TOKEN_TTL))
                    .withClaim("purpose", purpose)
                    .withClaim("ver", user.getTokenVersion());
            return customizer.apply(builder).sign(Algorithm.HMAC256(secret));
        } catch (JWTCreationException e) {
            throw new RuntimeException("Error creating JWT token", e);
        }
    }

    public String validateToken(String token) {
        DecodedJWT jwt = verify(token);
        return jwt == null ? "" : jwt.getSubject();
    }

    public int tokenVersion(DecodedJWT jwt) {
        Integer version = jwt.getClaim("ver").asInt();
        return version == null ? 0 : version;
    }

    private Instant generateExpirationDate() {
        return LocalDateTime
                .now()
                .plusHours(2)
                .toInstant(ZoneOffset.of("-03:00"));
    }
}
