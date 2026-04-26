package com.perm.starter.controller;

import com.perm.common.dto.Result;
import com.perm.common.entity.PermResource;
import com.perm.starter.service.PermResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 资源管理控制器
 */
@RestController
@RequestMapping("/perm-api/resources")
@RequiredArgsConstructor
public class PermResourceController {

    private final PermResourceService permResourceService;

    /**
     * 获取所有资源（按分组）
     */
    @GetMapping
    public Result<Map<String, List<PermResource>>> listByGroup() {
        return Result.success(permResourceService.listByGroup());
    }

    /**
     * 获取所有资源（平铺列表）
     */
    @GetMapping("/list")
    public Result<List<PermResource>> listAll() {
        return Result.success(permResourceService.listAll());
    }

    /**
     * 获取所有分组
     */
    @GetMapping("/groups")
    public Result<List<String>> listGroups() {
        return Result.success(permResourceService.listGroups());
    }

    /**
     * 重新扫描接口
     */
    @PostMapping("/scan")
    public Result<List<PermResource>> rescan() {
        return Result.success(permResourceService.rescan());
    }

    /**
     * 启用/禁用资源
     */
    @PutMapping("/{id}/toggle")
    public Result<PermResource> toggleEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        return Result.success(permResourceService.toggleEnabled(id, enabled));
    }
}
