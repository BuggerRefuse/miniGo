package org.example.domain.valueobject;

import org.example.domain.ports.PasswordEncoder;
import org.example.domain.ports.PasswordMatcher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordTest {

    private final PasswordEncoder encoder = raw -> "encoded:" + raw;
    private final PasswordMatcher matcher =
            (raw, encoded) -> encoded.equals("encoded:" + raw);

    @Test
    void matchesThePasswordThatWasEncoded() {
        Password password = Password.create("123456", encoder);

        assertTrue(password.matches("123456", matcher));
        assertFalse(password.matches("654321", matcher));
    }
}
