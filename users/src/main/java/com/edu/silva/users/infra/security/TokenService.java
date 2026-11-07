package com.edu.silva.users.infra.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.edu.silva.users.domain.entities.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    public String generateToken(User user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            List<String> roles = user.getRoles().stream()
                    .map(Enum::name)
                    .toList();
            return JWT.create()
                    .withIssuer("auth")
                    .withSubject(String.valueOf(user.getId()))
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
