package com.perm.server.controller;

import com.perm.common.dto.Result;
import com.perm.common.dto.RoleResourceRequest;
import com.perm.common.entity.PermResource;
import com.perm.common.entity.PermRole;
import com.perm.server.service.PermResourceService;
import com.perm.server.service.PermRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/perm-api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final PermRoleService permRoleService;
    private final PermResourceService permResourceService;

    @GetMapping
    public Result<List<PermRole>> list() { return Result.success(permRoleService.listAll()); }

    @GetMapping("/{id}")
    public Result<PermRole> getById(@PathVariable Long id) { return Result.success(permRoleService.getById(id)); }

    @PostMapping
    public Result<PermRole> create(@RequestBody PermRole role) { return Result.success(permRoleService.create(role)); }

    @PutMapping("/{id}")
    public Result<PermRole> update(@PathVariable Long id, @RequestBody PermRole role) {
        return Result.success(permRoleService.update(id, role));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) { permRoleService.delete(id); return Result.success(); }

    @GetMapping("/{id}/resources")
    public Result<List<Long>> getRoleResources(@PathVariable Long id) {
        return Result.success(permRoleService.getRoleResourceIds(id));
    }

    @PutMapping("/{id}/resources")
    public Result<Void> assignResources(@PathVariable Long id, @RequestBody RoleResourceRequest request) {
        if (request.getResourceIds() != null && !request.getResourceIds().isEmpty()) {
            List<PermResource> resources = permResourceService.listByIds(request.getResourceIds());
            if (resources.size() != request.getResourceIds().size()) {
                return Result.error(400, "包含无效的资源ID");
            }
        }
        permRoleService.assignResources(id, request.getResourceIds());
        return Result.success();
    }
}
