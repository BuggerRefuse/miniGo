package org.example.domain.ports;

@FunctionalInterface
public interface PasswordMatcher {

    boolean matches(String rawPassword, String encodedPassword);
}
