package org.example.admin.domain.valueobject;

import org.example.admin.domain.ports.PasswordEncoder;
import org.example.admin.domain.ports.PasswordMatcher;

import java.util.Objects;

public final class Password {

    private final String encodedValue;

    private Password(String encodedValue) {
        if (encodedValue == null || encodedValue.isBlank()) {
            throw new IllegalArgumentException("管理员密码不能为空");
        }
        this.encodedValue = encodedValue;
    }

    public static Password create(String rawPassword, PasswordEncoder encoder) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("管理员密码不能为空");
        }
        return new Password(Objects.requireNonNull(encoder).encode(rawPassword));
    }

    public static Password restore(String encodedValue) {
        return new Password(encodedValue);
    }

    public boolean matches(String rawPassword, PasswordMatcher matcher) {
        return rawPassword != null
                && !rawPassword.isBlank()
                && matcher.matches(rawPassword, encodedValue);
    }

    public String encodedValue() {
        return encodedValue;
    }
}
