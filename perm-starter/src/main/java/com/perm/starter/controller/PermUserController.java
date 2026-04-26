package com.perm.starter.controller;

import com.perm.common.dto.Result;
import com.perm.common.dto.ResetPasswordRequest;
import com.perm.common.dto.UserDTO;
import com.perm.common.dto.UserRoleRequest;
import com.perm.common.entity.PermUser;
import com.perm.starter.service.PermRoleService;
import com.perm.starter.service.PermUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/perm-api/users")
@RequiredArgsConstructor
public class PermUserController {

    private final PermUserService permUserService;
    private final PermRoleService permRoleService;

    /**
     * 获取所有用户（不返回密码）
     */
    @GetMapping
    public Result<List<UserDTO>> list() {
        List<PermUser> users = permUserService.listAll();
        List<UserDTO> dtos = users.stream().map(this::toDTO).collect(Collectors.toList());
        return Result.success(dtos);
    }

    /**
     * 获取用户详情（不返回密码）
     */
    @GetMapping("/{id}")
    public Result<UserDTO> getById(@PathVariable Long id) {
        PermUser user = permUserService.getById(id);
        return Result.success(toDTO(user));
    }

    /**
     * 创建用户
     */
    @PostMapping
    public Result<UserDTO> create(@RequestBody PermUser user) {
        PermUser created = permUserService.create(user);
        return Result.success(toDTO(created));
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    public Result<UserDTO> update(@PathVariable Long id, @RequestBody PermUser user) {
        PermUser updated = permUserService.update(id, user);
        return Result.success(toDTO(updated));
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        permUserService.delete(id);
        return Result.success();
    }

    /**
     * 重置密码（管理员操作）
     */
    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody ResetPasswordRequest request) {
        permUserService.resetPassword(id, request.getNewPassword());
        return Result.success();
    }

    /**
     * 获取用户角色ID
     */
    @GetMapping("/{id}/roles")
    public Result<List<Long>> getUserRoles(@PathVariable Long id) {
        return Result.success(permUserService.getUserRoleIds(id));
    }

    /**
     * 分配用户角色
     */
    @PutMapping("/{id}/roles")
    public Result<Void> assignRoles(@PathVariable Long id, @RequestBody UserRoleRequest request) {
        // 校验角色ID存在性
        if (request.getRoleIds() != null) {
            for (Long roleId : request.getRoleIds()) {
                permRoleService.getById(roleId); // 不存在会抛异常
            }
        }
        permUserService.assignRoles(id, request.getRoleIds());
        return Result.success();
    }

    private UserDTO toDTO(PermUser user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
