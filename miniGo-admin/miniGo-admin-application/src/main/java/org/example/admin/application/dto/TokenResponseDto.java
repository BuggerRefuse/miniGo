package org.example.admin.application.dto;

public record TokenResponseDto(
        String tokenType,
        String accessToken,
        long expiresIn,
        String refreshToken,
        AdminResponseDto principal
) {
}
