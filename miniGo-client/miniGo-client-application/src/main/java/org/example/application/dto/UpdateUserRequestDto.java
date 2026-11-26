package org.example.application.dto;

public record UpdateUserRequestDto(
        String password,
        String nickname,
        String avatarUrl,
        String phone,
        String email
) {
}
