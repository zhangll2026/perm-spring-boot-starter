package com.perm.starter.repository;

import com.perm.common.entity.PermRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermRoleRepository extends JpaRepository<PermRole, Long> {

    Optional<PermRole> findByCode(String code);

    List<PermRole> findByEnabledTrue();

    boolean existsByCode(String code);
}
