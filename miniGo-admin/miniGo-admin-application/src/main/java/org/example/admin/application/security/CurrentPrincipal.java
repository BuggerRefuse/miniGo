package org.example.admin.application.security;

public record CurrentPrincipal(
        Long id,
        String username,
        String displayName
) {
}
