package org.example.application.security;

import java.time.Instant;

public record RefreshTokenSession(
        String tokenId,
        Long principalId,
        Instant expiresAt
) {
}
