package com.edu.silva.gateway;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtAuthFilterTest {

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private ServerWebExchange exchange;
    @Mock
    private GatewayFilterChain chain;
    @Mock
    private ServerHttpRequest request;
    @Mock
    private ServerHttpResponse response;
    @Mock
    private Route route;
    @Mock
    private DecodedJWT jwt;
    @Mock
    private Claim rolesClaim;

    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter(jwtUtil);

        when(exchange.getRequest()).thenReturn(request);
        when(request.getURI()).thenReturn(URI.create("/finances/expenses"));
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(exchange.getResponse()).thenReturn(response);
        when(response.setComplete()).thenReturn(Mono.empty());
        when(route.getUri()).thenReturn(URI.create("lb://finances"));
        when(exchange.getAttribute(GATEWAY_ROUTE_ATTR)).thenReturn(route);
    }

    private void bearerToken(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        when(request.getHeaders()).thenReturn(headers);
    }

    @Test
    void shouldReturnUnauthorizedWhenAuthorizationHeaderIsMissing() {
        when(request.getHeaders()).thenReturn(new HttpHeaders());

        filter.filter(exchange, chain).block();

        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void shouldForbidWhenTokenWasIssuedForAnotherApp() {
        bearerToken("token");
        when(jwtUtil.validateToken("token")).thenReturn(jwt);
        when(jwt.getClaim("roles")).thenReturn(rolesClaim);
        when(rolesClaim.asList(String.class)).thenReturn(List.of("FINANCES"));
        when(jwt.getAudience()).thenReturn(List.of("STUDY"));

        filter.filter(exchange, chain).block();

        verify(response).setStatusCode(HttpStatus.FORBIDDEN);
        verify(chain, never()).filter(any());
    }

    @Test
    void shouldForbidWhenUserLacksRoleForTheApp() {
        bearerToken("token");
        when(jwtUtil.validateToken("token")).thenReturn(jwt);
        when(jwt.getClaim("roles")).thenReturn(rolesClaim);
        when(rolesClaim.asList(String.class)).thenReturn(List.of("STUDY"));
        when(jwt.getAudience()).thenReturn(List.of("FINANCES"));

        filter.filter(exchange, chain).block();

        verify(response).setStatusCode(HttpStatus.FORBIDDEN);
        verify(chain, never()).filter(any());
    }

    @Test
    void shouldAllowConfirmationLinkWithoutToken() {
        when(request.getURI()).thenReturn(URI.create("/confirm/00000000-0000-0000-0000-000000000000"));
        when(request.getHeaders()).thenReturn(new HttpHeaders());
        when(route.getUri()).thenReturn(URI.create("lb://users"));
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        verify(response, never()).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldForbidTasksWhenTokenIsFromAnotherApp() {
        when(request.getURI()).thenReturn(URI.create("/tasks"));
        when(route.getUri()).thenReturn(URI.create("lb://tasks"));
        bearerToken("token");
        when(jwtUtil.validateToken("token")).thenReturn(jwt);
        when(jwt.getClaim("roles")).thenReturn(rolesClaim);
        when(rolesClaim.asList(String.class)).thenReturn(List.of("FINANCES"));
        when(jwt.getAudience()).thenReturn(List.of("FINANCES"));

        filter.filter(exchange, chain).block();

        verify(response).setStatusCode(HttpStatus.FORBIDDEN);
        verify(chain, never()).filter(any());
    }
}
