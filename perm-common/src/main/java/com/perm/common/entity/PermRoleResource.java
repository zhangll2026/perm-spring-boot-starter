package com.perm.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 角色-资源关联实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "perm_role_resource", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"role_id", "resource_id"})
})
public class PermRoleResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;
}
