package org.example.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.example.gateway.config.GatewayJwtProperties;
import org.example.gateway.support.GatewayRequestAttributes;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    public static final String USER_ID_HEADER = "X-MiniGo-User-Id";
    public static final String USERNAME_HEADER = "X-MiniGo-Username";

    private static final String API_PREFIX = "/api";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final Set<String> PUBLIC_API_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/wechat",
            "/api/auth/refresh",
            "/api/auth/logout",
            "/api/users/register"
    );

    private final String issuer;
    private final SecretKey signingKey;

    public JwtAuthenticationGlobalFilter(GatewayJwtProperties properties) {
        if (properties.getSecret() == null
                || properties.getSecret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("网关 JWT 密钥长度不能少于 32 字节");
        }
        this.issuer = properties.getIssuer();
        this.signingKey = Keys.hmacShaKeyFor(
                properties.getSecret().getBytes(StandardCharsets.UTF_8)
        );
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest cleanRequest = removeClientIdentityHeaders(exchange.getRequest());
        ServerWebExchange cleanExchange = exchange.mutate().request(cleanRequest).build();
        String path = normalizePath(cleanRequest.getURI().getPath());

        if (!requiresAuthentication(cleanRequest, path)) {
            return chain.filter(cleanExchange);
        }

        String authorization = cleanRequest.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null
                || !authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return unauthorized(cleanExchange, "请先登录");
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!TOKEN_TYPE_ACCESS.equals(claims.get("type", String.class))
                    || claims.getSubject() == null
                    || claims.getSubject().isBlank()) {
                return unauthorized(cleanExchange, "令牌类型不正确");
            }

            cleanExchange.getAttributes().put(
                    GatewayRequestAttributes.USER_ID,
                    claims.getSubject()
            );
            ServerHttpRequest authenticatedRequest = cleanRequest.mutate()
                    .headers(headers -> {
                        headers.set(USER_ID_HEADER, claims.getSubject());
                        String username = claims.get("username", String.class);
                        if (username != null && !username.isBlank()) {
                            headers.set(USERNAME_HEADER, username);
                        }
                    })
                    .build();
            return chain.filter(cleanExchange.mutate().request(authenticatedRequest).build());
        } catch (JwtException | IllegalArgumentException exception) {
            return unauthorized(cleanExchange, "令牌无效或已过期");
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    private boolean requiresAuthentication(ServerHttpRequest request, String path) {
        if (HttpMethod.OPTIONS.equals(request.getMethod())) {
            return false;
        }
        if (!path.equals(API_PREFIX) && !path.startsWith(API_PREFIX + "/")) {
            return false;
        }
        return !PUBLIC_API_PATHS.contains(path);
    }

    private ServerHttpRequest removeClientIdentityHeaders(ServerHttpRequest request) {
        if (!request.getHeaders().containsHeader(USER_ID_HEADER)
                && !request.getHeaders().containsHeader(USERNAME_HEADER)) {
            return request;
        }
        return request.mutate()
                .headers(headers -> {
                    headers.remove(USER_ID_HEADER);
                    headers.remove(USERNAME_HEADER);
                })
                .build();
    }

    private String normalizePath(String path) {
        if (path.length() > 1 && path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().set(
                HttpHeaders.WWW_AUTHENTICATE,
                "Bearer"
        );
        byte[] body = ("{\"message\":\"" + message + "\"}")
                .getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
