package org.example.domain.ports;

import org.example.domain.aggregate.User;

import java.util.Optional;

public interface UserRepository {

    Optional<User> findById(Long id);

    Optional<User> findByUsername(String username);

    Optional<User> findByOpenId(String openId);

    User save(User user);

    void deleteById(Long id);
}
