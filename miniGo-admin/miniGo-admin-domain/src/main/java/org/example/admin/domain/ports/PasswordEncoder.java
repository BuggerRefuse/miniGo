package org.example.admin.domain.ports;

public interface PasswordEncoder {

    String encode(String rawPassword);
}
