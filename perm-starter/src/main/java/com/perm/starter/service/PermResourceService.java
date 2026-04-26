package com.perm.starter.service;

import com.perm.common.entity.PermResource;
import com.perm.common.exception.PermBizException;
import com.perm.starter.repository.PermResourceRepository;
import com.perm.starter.scanner.ApiScanner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 资源管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermResourceService {

    private final PermResourceRepository permResourceRepository;
    private final ApiScanner apiScanner;

    public List<PermResource> listAll() {
        return permResourceRepository.findAllOrderByGroupAndPath();
    }

    public List<String> listGroups() {
        return permResourceRepository.findAllGroupNames();
    }

    public Map<String, List<PermResource>> listByGroup() {
        List<PermResource> all = listAll();
        return all.stream().collect(Collectors.groupingBy(
                r -> r.getGroupName() != null ? r.getGroupName() : "未分组",
                LinkedHashMap::new,
                Collectors.toList()
        ));
    }

    /**
     * 重新扫描接口（清理无效接口 + 同步新增/更新）
     */
    @Transactional
    public List<PermResource> rescan() {
        apiScanner.scanAndPersist();
        return listAll();
    }

    @Transactional
    public PermResource toggleEnabled(Long id, boolean enabled) {
        PermResource resource = permResourceRepository.findById(id)
                .orElseThrow(() -> new PermBizException(404, "资源不存在: " + id));
        resource.setEnabled(enabled);
        return permResourceRepository.save(resource);
    }

    public Optional<PermResource> getById(Long id) {
        return permResourceRepository.findById(id);
    }

    public List<PermResource> listByIds(List<Long> ids) {
        return permResourceRepository.findAllById(ids);
    }
}
