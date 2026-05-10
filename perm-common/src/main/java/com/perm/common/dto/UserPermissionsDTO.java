package com.perm.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * 用户权限信息 DTO
 * 用于前端控制页面/按钮显隐
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPermissionsDTO {
    /** 用户 ID */
    private Long userId;
    /** 用户拥有的权限路径列表（格式：METHOD:PATH，如：GET:/api/products） */
    private Set<String> permissions;
    /** 用户拥有的角色列表 */
    private Set<String> roles;
}
