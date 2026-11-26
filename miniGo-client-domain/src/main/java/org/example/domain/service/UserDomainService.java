package org.example.domain.service;

import org.example.domain.aggregate.User;
import org.example.domain.ports.PasswordMatcher;

public class UserDomainService {

    public boolean login(User user, String rawPassword, PasswordMatcher matcher) {
        return user != null && user.canLoginWithPassword(rawPassword, matcher);
    }
}
