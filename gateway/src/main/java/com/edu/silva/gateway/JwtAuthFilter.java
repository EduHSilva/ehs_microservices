package com.edu.silva.gateway;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.edu.silva.common.Logger;
import com.edu.silva.common.enums.AppScope;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

@Component
public class JwtAuthFilter implements GlobalFilter {


    private static final org.slf4j.Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();
        Map<String, Object> meta = null;
        String ip = getClientIp(exchange);

        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);

        String service = null;

        if (route != null) {
            service = route.getUri().getHost();
        }

        // Rotas internas entre serviços nunca são expostas pelo gateway.
        if (path.startsWith("/internal") || path.contains("/internal/")) {
            exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
            return exchange.getResponse().setComplete();
        }

        // Rotas públicas: saúde, login/cadastro (o RewritePath do discovery roda antes
        // deste filtro, então o caminho já está sem o prefixo do serviço), a confirmação
        // de cadastro enviada por e-mail e o admin do Spring Boot.
        if (path.startsWith("/ping") || path.startsWith("/auth") || path.startsWith("/confirm")
                || path.startsWith("/budgets/public")
                || "admin".equalsIgnoreCase(service) || path.contains("swagger") || path.contains("/api-docs")) {
            Logger.log(null, method, path, service, meta, ip);
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        DecodedJWT jwt = jwtUtil.validateToken(token);
        if (jwt == null) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        List<String> roles = jwt.getClaim("roles").asList(String.class);

        boolean notificationsRoute = "notifications".equalsIgnoreCase(service)
                || (route != null && "notifications".equalsIgnoreCase(route.getId()));
        if (notificationsRoute && (path.startsWith("/admin") || path.startsWith("/notifications/admin"))
                && (roles == null || !roles.contains("ADMIN"))) {
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        Optional<AppScope> appScope = AppScope.fromService(service);
        // `tasks` é compartilhado pelo StudyFlow e pelo Orçamentos (lembretes). Sem app
        // próprio, o `aud` do token decide qual escopo a requisição assume; o header
        // X-App-Scope leva essa decisão ao serviço, que só devolve as tarefas da origem.
        String taskScope = null;
        if (appScope.isEmpty() && "tasks".equalsIgnoreCase(service)) {
            taskScope = resolveSharedScope(jwt.getAudience(), roles);
            if (taskScope == null) {
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }
        }
        if (appScope.isPresent()) {
            AppScope app = appScope.get();
            List<String> audience = jwt.getAudience();
            boolean audienceMatches = audience != null && audience.stream()
                    .anyMatch(aud -> app.name().equalsIgnoreCase(aud));

            // O token precisa ter sido emitido para esta aplicação (aud) e o
            // usuário precisa ter a role exigida por ela.
            if (!audienceMatches || !app.allows(roles)) {
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }
        }

        String subject = jwt.getSubject();
        String rolesHeader = roles == null ? "" : String.join(",", roles);
        String appScopeHeader = taskScope;

        exchange = exchange.mutate()
                .request(builder ->
                        builder.headers(headers -> {
                            headers.set("X-User-ID", subject);
                            headers.set("X-User-Roles", rolesHeader);
                            if (appScopeHeader != null) {
                                // Sobrescreve qualquer valor enviado pelo cliente.
                                headers.set("X-App-Scope", appScopeHeader);
                            }
                        }))
                .build();

        Logger.log(subject, method, path, service, meta, ip);
        return chain.filter(exchange);
    }

    /**
     * Escopo de uma requisição ao serviço compartilhado `tasks`: vale a aplicação que
     * emitiu o token (aud) e cuja role o usuário possui. Tokens de outras aplicações
     * continuam sem acesso.
     */
    private String resolveSharedScope(List<String> audience, List<String> roles) {
        for (AppScope app : List.of(AppScope.BUDGETS, AppScope.STUDY)) {
            boolean audienceMatches = audience != null && audience.stream()
                    .anyMatch(aud -> app.name().equalsIgnoreCase(aud));
            if (audienceMatches && app.allows(roles)) {
                return app.name();
            }
        }
        return null;
    }

    private String getClientIp(ServerWebExchange exchange) {
        try {
            String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");

            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                return xForwardedFor.split(",")[0].trim();
            }

            if (exchange.getRequest().getRemoteAddress() != null) {
                return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            }
        } catch (Exception e) {
            //
        }
        return null;
    }
}
