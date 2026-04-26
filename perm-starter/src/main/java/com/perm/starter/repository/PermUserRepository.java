package com.perm.starter.repository;

import com.perm.common.entity.PermUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermUserRepository extends JpaRepository<PermUser, Long> {

    Optional<PermUser> findByUsername(String username);

    boolean existsByUsername(String username);
}
