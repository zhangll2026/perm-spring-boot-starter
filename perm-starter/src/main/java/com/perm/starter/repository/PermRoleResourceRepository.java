package com.perm.starter.repository;

import com.perm.common.entity.PermRoleResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PermRoleResourceRepository extends JpaRepository<PermRoleResource, Long> {

    List<PermRoleResource> findByRoleId(Long roleId);

    List<PermRoleResource> findByResourceId(Long resourceId);

    void deleteByRoleId(Long roleId);

    void deleteByRoleIdAndResourceId(Long roleId, Long resourceId);

    @Query("SELECT rr FROM PermRoleResource rr WHERE rr.roleId IN :roleIds")
    List<PermRoleResource> findByRoleIds(@Param("roleIds") List<Long> roleIds);
}
