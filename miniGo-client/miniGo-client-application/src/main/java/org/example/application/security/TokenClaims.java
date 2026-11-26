package org.example.application.security;

import java.time.Instant;

public record TokenClaims(
        Long principalId,
        String username,
        String displayName,
        String avatarUrl,
        String tokenType,
        String tokenId,
        Instant expiresAt
) {
}
