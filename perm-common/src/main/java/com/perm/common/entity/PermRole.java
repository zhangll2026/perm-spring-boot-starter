package com.perm.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 角色实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "perm_role")
public class PermRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 角色编码，如 ADMIN、USER */
    @Column(nullable = false, unique = true, length = 64)
    private String code;

    /** 角色名称，如 管理员、普通用户 */
    @Column(nullable = false, length = 128)
    private String name;

    /** 角色描述 */
    @Column(length = 512)
    private String description;

    /** 是否启用 */
    @Column(nullable = false)
    private Boolean enabled = true;

    /** 创建时间 */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
