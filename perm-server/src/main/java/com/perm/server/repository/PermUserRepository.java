package com.perm.server.repository;

import com.perm.common.entity.PermUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermUserRepository extends JpaRepository<PermUser, Long> {
    Optional<PermUser> findByUsername(String username);
    boolean existsByUsername(String username);
}
