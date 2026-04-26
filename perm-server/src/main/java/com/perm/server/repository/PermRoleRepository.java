package com.perm.server.repository;

import com.perm.common.entity.PermRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermRoleRepository extends JpaRepository<PermRole, Long> {
    Optional<PermRole> findByCode(String code);
    boolean existsByCode(String code);
}
