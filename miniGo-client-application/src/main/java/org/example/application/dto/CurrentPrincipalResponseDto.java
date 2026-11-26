package org.example.application.dto;

public record CurrentPrincipalResponseDto(
        Long id,
        String username,
        String displayName,
        String avatarUrl
) {
}
