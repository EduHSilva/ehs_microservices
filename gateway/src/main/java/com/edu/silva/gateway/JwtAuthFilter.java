package com.edu.silva.gateway;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.edu.silva.common.Logger;
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

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

@Component
public class JwtAuthFilter implements GlobalFilter {


    private static final org.slf4j.Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    Map<String, String> routeRoles = Map.of(
            "crm", "CRM",
            "finances", "FINANCES",
            "fitness", "ROLE_FITNESS"
    );

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

        if (path.startsWith("/ping") || path.startsWith("/auth") || "admin".equalsIgnoreCase(service) || path.contains("swagger") || path.contains("/api-docs")) {
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

        for (Map.Entry<String, String> entry : routeRoles.entrySet()) {
            if (service != null && entry.getKey().equalsIgnoreCase(service) && (roles == null || !roles.contains("ADMIN"))) {
                if (roles == null || !roles.contains(entry.getValue())) {
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return exchange.getResponse().setComplete();
                }
            }
        }

        String subject = jwt.getSubject();

        exchange = exchange.mutate()
                .request(builder ->
                        builder.header("X-User-ID", subject))
                .build();

        Logger.log(subject, method, path, service, meta, ip);
        return chain.filter(exchange);
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
