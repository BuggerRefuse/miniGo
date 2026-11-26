package org.example.application.security;

public record CurrentPrincipal(
        Long id,
        String username,
        String displayName,
        String avatarUrl
) {
}
