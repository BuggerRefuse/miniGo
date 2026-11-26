package org.example.domain.service;

import org.example.domain.aggregate.User;
import org.example.domain.ports.PasswordEncoder;
import org.example.domain.ports.PasswordMatcher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserDomainServiceTest {

    private final PasswordEncoder encoder = raw -> "encoded:" + raw;
    private final PasswordMatcher matcher =
            (raw, encoded) -> encoded.equals("encoded:" + raw);

    @Test
    void registeredUserCanLoginWithTheCorrectPassword() {
        User user = User.create(1L, "alice", "123456", encoder);

        UserDomainService service = new UserDomainService();

        assertTrue(service.login(user, "123456", matcher));
        assertFalse(service.login(user, "654321", matcher));
    }
}
