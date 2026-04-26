package com.perm.starter.repository;

import com.perm.common.entity.PermResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermResourceRepository extends JpaRepository<PermResource, Long> {

    Optional<PermResource> findByPathAndMethod(String path, String method);

    List<PermResource> findByGroupName(String groupName);

    @Query("SELECT r FROM PermResource r ORDER BY r.groupName, r.path")
    List<PermResource> findAllOrderByGroupAndPath();

    List<PermResource> findByEnabledTrue();

    void deleteByPathAndMethod(String path, String method);

    @Query("SELECT DISTINCT r.groupName FROM PermResource r WHERE r.groupName IS NOT NULL ORDER BY r.groupName")
    List<String> findAllGroupNames();
}
