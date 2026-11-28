package org.example.admin.application.security;

import java.time.Instant;

public record TokenClaims(
        Long principalId,
        String username,
        String displayName,
        String tokenType,
        String tokenId,
        Instant expiresAt
) {
}
