package com.perm.starter.repository;

import com.perm.common.entity.PermUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PermUserRoleRepository extends JpaRepository<PermUserRole, Long> {

    List<PermUserRole> findByUserId(Long userId);

    List<PermUserRole> findByRoleId(Long roleId);

    void deleteByUserId(Long userId);

    void deleteByRoleId(Long roleId);

    void deleteByUserIdAndRoleId(Long userId, Long roleId);
}
