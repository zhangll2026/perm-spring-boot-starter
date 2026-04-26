package com.perm.server.controller;

import com.perm.common.dto.*;
import com.perm.common.entity.PermUser;
import com.perm.server.service.PermRoleService;
import com.perm.server.service.PermUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/perm-api/users")
@RequiredArgsConstructor
public class UserController {

    private final PermUserService permUserService;
    private final PermRoleService permRoleService;

    @GetMapping
    public Result<List<UserDTO>> list() {
        List<UserDTO> dtos = permUserService.listAll().stream().map(this::toDTO).collect(Collectors.toList());
        return Result.success(dtos);
    }

    @GetMapping("/{id}")
    public Result<UserDTO> getById(@PathVariable Long id) { return Result.success(toDTO(permUserService.getById(id))); }

    @PostMapping
    public Result<UserDTO> create(@RequestBody PermUser user) { return Result.success(toDTO(permUserService.create(user))); }

    @PutMapping("/{id}")
    public Result<UserDTO> update(@PathVariable Long id, @RequestBody PermUser user) {
        return Result.success(toDTO(permUserService.update(id, user)));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) { permUserService.delete(id); return Result.success(); }

    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody ResetPasswordRequest request) {
        // 简化：直接用新密码
        PermUser user = permUserService.getById(id);
        PermUser update = new PermUser();
        update.setPassword(request.getNewPassword());
        permUserService.update(id, update);
        return Result.success();
    }

    @GetMapping("/{id}/roles")
    public Result<List<Long>> getUserRoles(@PathVariable Long id) {
        return Result.success(permUserService.getUserRoleIds(id));
    }

    @PutMapping("/{id}/roles")
    public Result<Void> assignRoles(@PathVariable Long id, @RequestBody UserRoleRequest request) {
        if (request.getRoleIds() != null) {
            for (Long roleId : request.getRoleIds()) permRoleService.getById(roleId);
        }
        permUserService.assignRoles(id, request.getRoleIds());
        return Result.success();
    }

    private UserDTO toDTO(PermUser user) {
        return UserDTO.builder()
                .id(user.getId()).username(user.getUsername())
                .nickname(user.getNickname()).enabled(user.getEnabled())
                .createdAt(user.getCreatedAt()).updatedAt(user.getUpdatedAt()).build();
    }
}
