package org.example.application.dto;

import org.example.domain.aggregate.User;
import org.example.domain.valueobject.AccountStatus;

public record UserResponseDto(
        Long id,
        String username,
        String nickname,
        String avatarUrl,
        String phone,
        String email,
        boolean wechatBound,
        boolean enabled
) {
    public static UserResponseDto from(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getPhone(),
                user.getEmail(),
                user.getOpenId() != null,
                user.getStatus() == AccountStatus.ACTIVE
        );
    }
}
