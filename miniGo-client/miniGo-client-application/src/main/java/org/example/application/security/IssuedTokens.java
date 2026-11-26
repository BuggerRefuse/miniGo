package org.example.application.security;

import java.time.Instant;

public record IssuedTokens(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresInSeconds,
        String refreshTokenId,
        Instant refreshTokenExpiresAt
) {
}
