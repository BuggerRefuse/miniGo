package org.example.application.security;

import org.example.domain.ports.PasswordEncoder;
import org.example.domain.ports.PasswordMatcher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BcryptPasswordCodec implements PasswordEncoder, PasswordMatcher {

    private final BCryptPasswordEncoder delegate = new BCryptPasswordEncoder(10);

    @Override
    public String encode(String rawPassword) {
        return delegate.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return rawPassword != null
                && encodedPassword != null
                && !encodedPassword.isBlank()
                && delegate.matches(rawPassword, encodedPassword);
    }
}
