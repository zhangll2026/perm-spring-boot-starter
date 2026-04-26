package com.perm.server.controller;

import com.perm.common.dto.Result;
import com.perm.common.entity.PermResource;
import com.perm.server.service.PermResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/perm-api/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final PermResourceService permResourceService;

    @GetMapping
    public Result<Map<String, Map<String, List<PermResource>>>> listByServiceAndGroup() {
        return Result.success(permResourceService.listByServiceAndGroup());
    }

    @GetMapping("/list")
    public Result<List<PermResource>> listAll() { return Result.success(permResourceService.listAll()); }

    @GetMapping("/services")
    public Result<List<String>> listServiceIds() { return Result.success(permResourceService.listServiceIds()); }

    @PutMapping("/{id}/toggle")
    public Result<PermResource> toggleEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        return Result.success(permResourceService.toggleEnabled(id, enabled));
    }

    /**
     * 微服务客户端注册接口（内部调用）
     */
    @PostMapping("/register")
    public Result<Void> register(@RequestParam String serviceId, @RequestBody List<PermResource> resources) {
        permResourceService.registerResources(serviceId, resources);
        return Result.success();
    }
}
