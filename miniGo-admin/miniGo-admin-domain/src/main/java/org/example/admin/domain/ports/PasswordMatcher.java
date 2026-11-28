package org.example.admin.domain.ports;

public interface PasswordMatcher {

    boolean matches(String rawPassword, String encodedPassword);
}
