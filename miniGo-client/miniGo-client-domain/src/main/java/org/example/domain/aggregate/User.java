package org.example.domain.aggregate;

import lombok.AllArgsConstructor;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import org.example.domain.ports.PasswordEncoder;
import org.example.domain.ports.PasswordMatcher;
import org.example.domain.valueobject.AccountStatus;
import org.example.domain.valueobject.Password;

import java.util.Objects;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class User {

    private Long id;
    private final String username;
    @Getter(AccessLevel.NONE)
    private final Password password;
    private final String openId;
    private final String nickname;
    private final String avatarUrl;
    private final String phone;
    private final String email;
    @NonNull
    private final AccountStatus status;

    public static User create(
            Long id,
            String username,
            String rawPassword,
            PasswordEncoder encoder
    ) {
        return new User(
                id,
                normalizeUsername(username),
                Password.create(rawPassword, encoder),
                null,
                normalizeUsername(username),
                null,
                null,
                null,
                AccountStatus.ACTIVE
        );
    }

    public static User createPasswordAccount(
            String username,
            String rawPassword,
            String nickname,
            String phone,
            String email,
            PasswordEncoder encoder
    ) {
        String normalizedUsername = normalizeUsername(username);
        return new User(
                null,
                normalizedUsername,
                Password.create(rawPassword, encoder),
                null,
                normalizeNickname(nickname, normalizedUsername),
                null,
                emptyToNull(phone),
                emptyToNull(email),
                AccountStatus.ACTIVE
        );
    }

    public static User createWeChatAccount(
            String username,
            String openId,
            String nickname,
            String avatarUrl
    ) {
        String normalizedUsername = normalizeUsername(username);
        if (openId == null || openId.isBlank()) {
            throw new IllegalArgumentException("微信 openId 不能为空");
        }

        return new User(
                null,
                normalizedUsername,
                null,
                openId.trim(),
                normalizeNickname(nickname, normalizedUsername),
                emptyToNull(avatarUrl),
                null,
                null,
                AccountStatus.ACTIVE
        );
    }

    public static User restore(
            Long id,
            String username,
            String encodedPassword,
            String openId,
            String nickname,
            String avatarUrl,
            String phone,
            String email,
            AccountStatus status
    ) {
        Objects.requireNonNull(id, "user id must not be null");
        return new User(
                id,
                normalizeUsername(username),
                encodedPassword == null ? null : Password.restore(encodedPassword),
                emptyToNull(openId),
                normalizeNickname(nickname, username),
                emptyToNull(avatarUrl),
                emptyToNull(phone),
                emptyToNull(email),
                Objects.requireNonNull(status, "status must not be null")
        );
    }

    public User assignId(Long id) {
        if (this.id != null) {
            throw new IllegalStateException("user id has already been assigned");
        }
        this.id = Objects.requireNonNull(id, "user id must not be null");
        return this;
    }

    public User updateProfile(
            String nickname,
            String avatarUrl,
            String phone,
            String email
    ) {
        return new User(
                id,
                username,
                password,
                openId,
                normalizeNickname(nickname, username),
                avatarUrl,
                emptyToNull(phone),
                emptyToNull(email),
                status
        );
    }

    public User changePassword(String rawPassword, PasswordEncoder encoder) {
        return new User(
                id,
                username,
                Password.create(rawPassword, encoder),
                openId,
                nickname,
                avatarUrl,
                phone,
                email,
                status
        );
    }

    public User changeStatus(AccountStatus status) {
        return new User(
                id,
                username,
                password,
                openId,
                nickname,
                avatarUrl,
                phone,
                email,
                Objects.requireNonNull(status, "status must not be null")
        );
    }

    public boolean matchesPassword(String rawPassword, PasswordMatcher matcher) {
        return password != null && password.matches(rawPassword, matcher);
    }

    public boolean canLoginWithPassword(String rawPassword, PasswordMatcher matcher) {
        return status == AccountStatus.ACTIVE && matchesPassword(rawPassword, matcher);
    }

    public String getEncodedPassword() {
        return password == null ? null : password.encodedValue();
    }

    private static String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        return username.trim();
    }

    private static String normalizeNickname(String nickname, String fallback) {
        return nickname == null || nickname.isBlank() ? fallback : nickname.trim();
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
