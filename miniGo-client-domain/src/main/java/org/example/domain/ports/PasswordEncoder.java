package org.example.domain.ports;

@FunctionalInterface
public interface PasswordEncoder {

    String encode(String rawPassword);
}
