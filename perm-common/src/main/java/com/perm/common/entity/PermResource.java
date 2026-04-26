package com.perm.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * API资源实体 - 存储扫描到的接口信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "perm_resource")
public class PermResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 接口路径，如 /api/user/list */
    @Column(nullable = false, length = 255)
    private String path;

    /** 请求方法：GET/POST/PUT/DELETE等 */
    @Column(nullable = false, length = 20)
    private String method;

    /** 接口描述，优先取 @ApiOperation/@Operation 注解，否则取方法名 */
    @Column(length = 255)
    private String description;

    /** 所属Controller类名 */
    @Column(name = "controller_class", length = 255)
    private String controllerClass;

    /** 方法名 */
    @Column(name = "method_name", length = 255)
    private String methodName;

    /** 分组名称，取Controller类上的注解或类名简写 */
    @Column(name = "group_name", length = 255)
    private String groupName;

    /** 所属服务ID（微服务场景下标识接口来源服务） */
    @Column(name = "service_id", length = 128)
    private String serviceId;

    /** 是否启用权限控制 */
    @Column(name = "enabled")
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
