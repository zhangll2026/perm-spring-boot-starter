package com.perm.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户角色分配请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleRequest {

    /** 角色ID列表 */
    private java.util.List<Long> roleIds;
}
