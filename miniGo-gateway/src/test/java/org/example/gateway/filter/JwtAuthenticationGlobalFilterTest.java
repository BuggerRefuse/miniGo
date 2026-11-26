package org.example.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.example.gateway.config.GatewayJwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationGlobalFilterTest {

    private static final String SECRET =
            "gateway-test-secret-with-at-least-32-bytes";
    private static final String ISSUER = "mini-go-client";

    private JwtAuthenticationGlobalFilter filter;
    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        GatewayJwtProperties properties = new GatewayJwtProperties();
        properties.setSecret(SECRET);
        properties.setIssuer(ISSUER);
        filter = new JwtAuthenticationGlobalFilter(properties);
        signingKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void rejectsProtectedApiWithoutAccessToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/users/me").build()
        );

        StepVerifier.create(filter.filter(exchange, failingChain()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsRefreshTokenOnProtectedApi() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/users/me")
                        .header("Authorization", "Bearer " + token("refresh"))
                        .build()
        );

        StepVerifier.create(filter.filter(exchange, failingChain()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void forwardsValidAccessTokenAndReplacesSpoofedIdentityHeaders() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/users/me")
                        .header("Authorization", "Bearer " + token("access"))
                        .header(JwtAuthenticationGlobalFilter.USER_ID_HEADER, "999")
                        .header(JwtAuthenticationGlobalFilter.USERNAME_HEADER, "attacker")
                        .build()
        );
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        GatewayFilterChain chain = forwardedExchange -> {
            forwarded.set(forwardedExchange);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(forwarded.get()).isNotNull();
        assertThat(forwarded.get().getRequest().getHeaders()
                .getFirst(JwtAuthenticationGlobalFilter.USER_ID_HEADER))
                .isEqualTo("42");
        assertThat(forwarded.get().getRequest().getHeaders()
                .getFirst(JwtAuthenticationGlobalFilter.USERNAME_HEADER))
                .isEqualTo("demo");
    }

    @Test
    void allowsPublicLoginPathWithoutToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/auth/login").build()
        );
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        StepVerifier.create(filter.filter(exchange, forwardedExchange -> {
                    forwarded.set(forwardedExchange);
                    return Mono.empty();
                }))
                .verifyComplete();

        assertThat(forwarded.get()).isNotNull();
    }

    private GatewayFilterChain failingChain() {
        return exchange -> Mono.error(
                new AssertionError("Gateway filter chain must not be reached")
        );
    }

    private String token(String type) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject("42")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(300)))
                .claim("type", type)
                .claim("username", "demo")
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }
}
