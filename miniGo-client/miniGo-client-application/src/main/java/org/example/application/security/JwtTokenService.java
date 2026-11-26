package org.example.application.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.example.application.exception.InvalidTokenException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenService {

    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final JwtProperties properties;
    private final Clock clock;
    private final SecretKey signingKey;

    public JwtTokenService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        byte[] secret = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalArgumentException("JWT 密钥长度不能少于 32 字节");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret);
    }

    public IssuedTokens issueTokens(CurrentPrincipal principal) {
        Instant issuedAt = clock.instant();
        Instant accessExpiresAt = issuedAt.plus(properties.getAccessTtl());
        Instant refreshExpiresAt = issuedAt.plus(properties.getRefreshTtl());
        String refreshTokenId = UUID.randomUUID().toString();

        String accessToken = buildToken(
                principal,
                TOKEN_TYPE_ACCESS,
                UUID.randomUUID().toString(),
                issuedAt,
                accessExpiresAt
        );
        String refreshToken = buildToken(
                principal,
                TOKEN_TYPE_REFRESH,
                refreshTokenId,
                issuedAt,
                refreshExpiresAt
        );

        return new IssuedTokens(
                accessToken,
                refreshToken,
                properties.getAccessTtl().toSeconds(),
                refreshTokenId,
                refreshExpiresAt
        );
    }

    public TokenClaims parseAccessToken(String token) {
        return parse(token, TOKEN_TYPE_ACCESS);
    }

    public TokenClaims parseRefreshToken(String token) {
        return parse(token, TOKEN_TYPE_REFRESH);
    }

    private String buildToken(
            CurrentPrincipal principal,
            String tokenType,
            String tokenId,
            Instant issuedAt,
            Instant expiresAt
    ) {
        return Jwts.builder()
                .issuer(properties.getIssuer())
                .subject(String.valueOf(principal.id()))
                .id(tokenId)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .claim("type", tokenType)
                .claim("username", principal.username())
                .claim("displayName", principal.displayName())
                .claim("avatarUrl", principal.avatarUrl())
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    private TokenClaims parse(String token, String expectedType) {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("令牌不能为空");
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(properties.getIssuer())
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String tokenType = claims.get("type", String.class);
            if (!expectedType.equals(tokenType)) {
                throw new InvalidTokenException("令牌类型不正确");
            }

            return new TokenClaims(
                    Long.valueOf(claims.getSubject()),
                    claims.get("username", String.class),
                    claims.get("displayName", String.class),
                    claims.get("avatarUrl", String.class),
                    tokenType,
                    claims.getId(),
                    claims.getExpiration().toInstant()
            );
        } catch (InvalidTokenException exception) {
            throw exception;
        } catch (JwtException | IllegalArgumentException exception) {
            throw new InvalidTokenException("令牌无效或已过期");
        }
    }
}
