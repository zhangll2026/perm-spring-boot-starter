package com.perm.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 角色资源分配请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleResourceRequest {

    /** 资源ID列表 */
    private java.util.List<Long> resourceIds;
}
