package org.example.application.dto;

public record TokenResponseDto(
        String tokenType,
        String accessToken,
        long expiresIn,
        String refreshToken,
        CurrentPrincipalResponseDto principal
) {
}
