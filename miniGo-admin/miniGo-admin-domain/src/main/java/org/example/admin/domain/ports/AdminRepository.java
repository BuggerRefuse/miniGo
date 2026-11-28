package org.example.admin.domain.ports;

import org.example.admin.domain.aggregate.Admin;

import java.util.Optional;

public interface AdminRepository {

    Optional<Admin> findById(Long id);

    Optional<Admin> findByUsername(String username);
}
