package org.example.admin.domain.aggregate;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.admin.domain.ports.PasswordEncoder;
import org.example.admin.domain.ports.PasswordMatcher;
import org.example.admin.domain.valueobject.AdminStatus;
import org.example.admin.domain.valueobject.Password;

import java.util.Objects;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class Admin {

    private Long id;
    private final String username;
    @Getter(AccessLevel.NONE)
    private final Password password;
    private final String displayName;
    private final AdminStatus status;

    public static Admin create(
            String username,
            String rawPassword,
            String displayName,
            PasswordEncoder encoder
    ) {
        String normalizedUsername = normalizeUsername(username);
        return new Admin(
                null,
                normalizedUsername,
                Password.create(rawPassword, encoder),
                normalizeDisplayName(displayName, normalizedUsername),
                AdminStatus.ACTIVE
        );
    }

    public static Admin restore(
            Long id,
            String username,
            String encodedPassword,
            String displayName,
            AdminStatus status
    ) {
        return new Admin(
                Objects.requireNonNull(id, "admin id must not be null"),
                normalizeUsername(username),
                Password.restore(encodedPassword),
                normalizeDisplayName(displayName, username),
                Objects.requireNonNull(status, "admin status must not be null")
        );
    }

    public boolean canLogin(String rawPassword, PasswordMatcher matcher) {
        return status == AdminStatus.ACTIVE && password.matches(rawPassword, matcher);
    }

    public String getEncodedPassword() {
        return password.encodedValue();
    }

    public Admin assignId(Long id) {
        if (this.id != null) {
            throw new IllegalStateException("admin id has already been assigned");
        }
        this.id = Objects.requireNonNull(id, "admin id must not be null");
        return this;
    }

    private static String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("管理员用户名不能为空");
        }
        return username.trim();
    }

    private static String normalizeDisplayName(String displayName, String fallback) {
        return displayName == null || displayName.isBlank()
                ? fallback.trim()
                : displayName.trim();
    }
}
