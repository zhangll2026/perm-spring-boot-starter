package com.perm.server.repository;

import com.perm.common.entity.PermUserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PermUserRoleRepository extends JpaRepository<PermUserRole, Long> {
    List<PermUserRole> findByUserId(Long userId);
    List<PermUserRole> findByRoleId(Long roleId);
    void deleteByUserId(Long userId);
    void deleteByRoleId(Long roleId);
}
