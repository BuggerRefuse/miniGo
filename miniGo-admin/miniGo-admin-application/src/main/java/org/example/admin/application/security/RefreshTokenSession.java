package org.example.admin.application.security;

import java.time.Instant;

public record RefreshTokenSession(
        String tokenId,
        Long principalId,
        Instant expiresAt
) {
}
