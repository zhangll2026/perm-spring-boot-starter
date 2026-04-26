package com.perm.starter.service;

import com.perm.common.entity.PermRole;
import com.perm.common.entity.PermRoleResource;
import com.perm.common.exception.PermBizException;
import com.perm.starter.repository.PermRoleRepository;
import com.perm.starter.repository.PermRoleResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermRoleService {

    private final PermRoleRepository permRoleRepository;
    private final PermRoleResourceRepository permRoleResourceRepository;
    private final PermAuthService permAuthService;

    public List<PermRole> listAll() {
        return permRoleRepository.findAll();
    }

    public PermRole getById(Long id) {
        return permRoleRepository.findById(id)
                .orElseThrow(() -> new PermBizException(404, "角色不存在: " + id));
    }

    @Transactional
    public PermRole create(PermRole role) {
        if (permRoleRepository.existsByCode(role.getCode())) {
            throw new PermBizException(400, "角色编码已存在: " + role.getCode());
        }
        return permRoleRepository.save(role);
    }

    @Transactional
    public PermRole update(Long id, PermRole roleUpdate) {
        PermRole role = getById(id);
        if (roleUpdate.getCode() != null) {
            PermRole existing = permRoleRepository.findByCode(roleUpdate.getCode()).orElse(null);
            if (existing != null && !existing.getId().equals(id)) {
                throw new PermBizException(400, "角色编码已存在: " + roleUpdate.getCode());
            }
            role.setCode(roleUpdate.getCode());
        }
        if (roleUpdate.getName() != null) {
            role.setName(roleUpdate.getName());
        }
        if (roleUpdate.getDescription() != null) {
            role.setDescription(roleUpdate.getDescription());
        }
        if (roleUpdate.getEnabled() != null) {
            role.setEnabled(roleUpdate.getEnabled());
        }
        return permRoleRepository.save(role);
    }

    @Transactional
    public void delete(Long id) {
        permRoleResourceRepository.deleteByRoleId(id);
        permRoleRepository.deleteById(id);
        permAuthService.clearAllPermissionCache();
    }

    public List<Long> getRoleResourceIds(Long roleId) {
        return permRoleResourceRepository.findByRoleId(roleId).stream()
                .map(PermRoleResource::getResourceId)
                .collect(Collectors.toList());
    }

    @Transactional
    public void assignResources(Long roleId, List<Long> resourceIds) {
        permRoleResourceRepository.deleteByRoleId(roleId);
        if (resourceIds != null && !resourceIds.isEmpty()) {
            List<PermRoleResource> newRelations = resourceIds.stream()
                    .map(resourceId -> PermRoleResource.builder()
                            .roleId(roleId)
                            .resourceId(resourceId)
                            .build())
                    .collect(Collectors.toList());
            permRoleResourceRepository.saveAll(newRelations);
        }
        permAuthService.clearAllPermissionCache();
    }
}
