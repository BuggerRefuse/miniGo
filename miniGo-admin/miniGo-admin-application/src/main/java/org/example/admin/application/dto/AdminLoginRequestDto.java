package org.example.admin.application.dto;

public record AdminLoginRequestDto(
        String username,
        String password
) {
}
