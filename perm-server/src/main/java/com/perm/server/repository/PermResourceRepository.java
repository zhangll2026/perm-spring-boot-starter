package com.perm.server.repository;

import com.perm.common.entity.PermResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PermResourceRepository extends JpaRepository<PermResource, Long> {

    Optional<PermResource> findByPathAndMethodAndServiceId(String path, String method, String serviceId);

    List<PermResource> findByServiceId(String serviceId);

    @Query("SELECT r FROM PermResource r ORDER BY r.serviceId, r.groupName, r.path")
    List<PermResource> findAllOrderByServiceAndGroupAndPath();

    List<PermResource> findByEnabledTrue();

    @Query("SELECT DISTINCT r.serviceId FROM PermResource r WHERE r.serviceId IS NOT NULL ORDER BY r.serviceId")
    List<String> findAllServiceIds();

    @Query("SELECT DISTINCT r.groupName FROM PermResource r WHERE r.groupName IS NOT NULL ORDER BY r.groupName")
    List<String> findAllGroupNames();

    void deleteByServiceIdAndPathAndMethod(String serviceId, String path, String method);
}
