package org.example.domain.valueobject;

import org.example.domain.ports.PasswordEncoder;
import org.example.domain.ports.PasswordMatcher;

import java.util.Objects;

public final class Password {

    private static final int MIN_LENGTH = 6;

    private final String encryptedValue;

    private Password(String encryptedValue) {
        this.encryptedValue = encryptedValue;
    }

    public static Password create(String rawPassword, PasswordEncoder encoder) {
        validateRawPassword(rawPassword);
        Objects.requireNonNull(encoder, "password encoder must not be null");

        return new Password(encoder.encode(rawPassword));
    }

    public static Password restore(String encodedValue) {
        if (encodedValue == null || encodedValue.isBlank()) {
            throw new IllegalArgumentException("密码摘要不能为空");
        }
        return new Password(encodedValue);
    }

    public boolean matches(String rawPassword, PasswordMatcher matcher) {
        if (rawPassword == null || rawPassword.isBlank()) {
            return false;
        }
        Objects.requireNonNull(matcher, "password matcher must not be null");

        return matcher.matches(rawPassword, encryptedValue);
    }

    public String encodedValue() {
        return encryptedValue;
    }

    private static void validateRawPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("密码长度不能小于6位");
        }
    }
}
