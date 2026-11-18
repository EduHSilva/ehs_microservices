package com.silva.edu.finances.infra.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Best-effort lookup of user profiles in the users service. Sharing keeps working
 * when the service is unreachable; only e-mail verification and display names
 * are skipped in that case.
 */
@Component
public class UsersServiceClient {
    private static final Logger log = LoggerFactory.getLogger(UsersServiceClient.class);

    public record Profile(String id, String username, String email) {}
    private record Envelope<T>(String message, T data) {}

    private final RestClient client;

    public UsersServiceClient(@Value("${finances.users-service-url:}") String baseUrl,
                              @Value("${finances.internal-token:}") String internalToken) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(3));
        this.client = baseUrl == null || baseUrl.isBlank() ? null
                : RestClient.builder()
                        .baseUrl(baseUrl.trim())
                        .defaultHeader("X-Internal-Token", internalToken == null ? "" : internalToken)
                        .requestFactory(factory)
                        .build();
    }

    public Optional<Profile> findById(UUID id) {
        if (client == null || id == null) return Optional.empty();
        try {
            Envelope<List<Profile>> response = client.get()
                    .uri("/internal/profiles?ids={id}", id)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Envelope<List<Profile>>>() {});
            if (response == null || response.data() == null || response.data().isEmpty()) return Optional.empty();
            return Optional.of(response.data().get(0));
        } catch (RestClientResponseException ex) {
            log.warn("users service lookup failed for {}: {}", id, ex.getStatusCode());
            return Optional.empty();
        } catch (RuntimeException ex) {
            log.warn("users service unavailable for {}: {}", id, ex.getMessage());
            return Optional.empty();
        }
    }

    public Optional<Profile> findByEmail(String email) {
        if (client == null || email == null || email.isBlank()) return Optional.empty();
        try {
            Envelope<Profile> response = client.get()
                    .uri("/internal/profiles/by-email?email={email}", email.trim().toLowerCase())
                    .retrieve()
                    .body(new ParameterizedTypeReference<Envelope<Profile>>() {});
            return response == null ? Optional.empty() : Optional.ofNullable(response.data());
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) return Optional.empty();
            log.warn("users service lookup failed for {}: {}", email, ex.getStatusCode());
            return Optional.empty();
        } catch (RuntimeException ex) {
            log.warn("users service unavailable for {}: {}", email, ex.getMessage());
            return Optional.empty();
        }
    }
}
