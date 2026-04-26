package com.perm.server.service;

import com.perm.common.entity.PermResource;
import com.perm.common.exception.PermBizException;
import com.perm.server.repository.PermResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermResourceService {

    private final PermResourceRepository permResourceRepository;

    public List<PermResource> listAll() {
        return permResourceRepository.findAllOrderByServiceAndGroupAndPath();
    }

    public List<String> listServiceIds() {
        return permResourceRepository.findAllServiceIds();
    }

    public Map<String, Map<String, List<PermResource>>> listByServiceAndGroup() {
        List<PermResource> all = listAll();
        return all.stream().collect(Collectors.groupingBy(
                r -> r.getServiceId() != null ? r.getServiceId() : "未分组",
                LinkedHashMap::new,
                Collectors.groupingBy(
                        r -> r.getGroupName() != null ? r.getGroupName() : "未分组",
                        LinkedHashMap::new,
                        Collectors.toList()
                )
        ));
    }

    public List<PermResource> listByIds(List<Long> ids) {
        return permResourceRepository.findAllById(ids);
    }

    @Transactional
    public PermResource toggleEnabled(Long id, boolean enabled) {
        PermResource r = permResourceRepository.findById(id)
                .orElseThrow(() -> new PermBizException(404, "资源不存在: " + id));
        r.setEnabled(enabled);
        return permResourceRepository.save(r);
    }

    /**
     * 微服务客户端注册接口到服务端
     */
    @Transactional
    public void registerResources(String serviceId, List<PermResource> resources) {
        log.info("[PermServer] 接收服务 {} 注册 {} 个接口", serviceId, resources.size());

        // 先加载该服务现有的所有资源
        List<PermResource> existing = permResourceRepository.findByServiceId(serviceId);
        Map<String, PermResource> existingMap = existing.stream()
                .collect(Collectors.toMap(
                        r -> r.getMethod().toUpperCase() + ":" + r.getPath(),
                        r -> r, (a, b) -> a));

        Set<String> currentKeys = new HashSet<>();
        List<PermResource> newResources = new ArrayList<>();

        for (PermResource res : resources) {
            res.setServiceId(serviceId);
            String key = res.getMethod().toUpperCase() + ":" + res.getPath();
            currentKeys.add(key);

            PermResource ex = existingMap.get(key);
            if (ex != null) {
                ex.setDescription(res.getDescription());
                ex.setControllerClass(res.getControllerClass());
                ex.setMethodName(res.getMethodName());
                ex.setGroupName(res.getGroupName());
            } else {
                res.setEnabled(true);
                newResources.add(res);
            }
        }

        // 清理不再存在的旧接口
        Set<String> removedKeys = new HashSet<>(existingMap.keySet());
        removedKeys.removeAll(currentKeys);
        for (String key : removedKeys) {
            PermResource removed = existingMap.get(key);
            permResourceRepository.deleteById(removed.getId());
        }

        if (!newResources.isEmpty()) {
            permResourceRepository.saveAll(newResources);
        }
        if (!removedKeys.isEmpty()) {
            log.info("[PermServer] 服务 {} 清理了 {} 个无效接口", serviceId, removedKeys.size());
        }
    }
}
