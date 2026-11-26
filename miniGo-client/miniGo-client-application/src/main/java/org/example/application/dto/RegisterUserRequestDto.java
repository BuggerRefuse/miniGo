package org.example.application.dto;

public record RegisterUserRequestDto(
        String username,
        String password,
        String nickname,
        String phone,
        String email
) {
}
