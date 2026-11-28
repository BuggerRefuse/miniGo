package org.example.admin.application.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.example.admin.application.exception.InvalidTokenException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {

    private static final String SECRET = "admin-test-secret-with-at-least-32-bytes";
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-11-27T02:00:00Z"), ZoneOffset.UTC);

    @Test
    void issuedAccessTokenContainsAdminPrincipalType() {
        JwtTokenService service = service();

        String token = service.issueTokens(
                new CurrentPrincipal(7L, "admin", "系统管理员")
        ).accessToken();

        assertThat(service.parseAccessToken(token).principalId()).isEqualTo(7L);
    }

    @Test
    void rejectsUserTokenWithoutAdminPrincipalType() {
        JwtTokenService service = service();
        String userToken = Jwts.builder()
                .issuer("mini-go-client")
                .subject("1")
                .issuedAt(Date.from(CLOCK.instant()))
                .expiration(Date.from(CLOCK.instant().plusSeconds(300)))
                .claim("type", "access")
                .claim("username", "demo")
                .signWith(
                        Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)),
                        Jwts.SIG.HS256
                )
                .compact();

        assertThatThrownBy(() -> service.parseAccessToken(userToken))
                .isInstanceOf(InvalidTokenException.class);
    }

    private JwtTokenService service() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setIssuer("mini-go-client");
        return new JwtTokenService(properties, CLOCK);
    }
}
