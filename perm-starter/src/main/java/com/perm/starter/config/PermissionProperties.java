package com.perm.starter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限管理配置属性
 */
@Data
@ConfigurationProperties(prefix = "perm")
public class PermissionProperties {

    /** 是否启用权限拦截（默认启用） */
    private boolean enabled = true;

    /** 是否允许匿名访问（未登录时是否放行，默认不允许） */
    private boolean allowAnonymous = false;

    /** JWT密钥（必须配置，至少32字符，不配置则启动失败） */
    private String jwtSecret;

    /** JWT过期时间（毫秒），默认24小时 */
    private long jwtExpiration = 86400000L;

    /** 权限管理页面路径前缀 */
    private String adminPath = "/perm-admin";

    /** API接口路径前缀 */
    private String apiPath = "/perm-api";

    /** 放行路径列表（不需要权限校验的路径，支持AntPath） */
    private List<String> excludePaths = new ArrayList<>();

    /** 是否自动扫描接口（默认启用） */
    private boolean autoScan = true;

    /** 是否初始化默认管理员（默认启用） */
    private boolean initAdmin = true;

    /** 默认管理员密码（未配置则自动生成随机密码） */
    private String adminPassword;

    /** CORS允许的来源域名列表，默认只允许同源 */
    private List<String> allowedOrigins = new ArrayList<>();

    /**
     * 校验JWT密钥配置
     */
    public void validateJwtSecret() {
        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalStateException(
                "[PermStarter] 必须配置 perm.jwt-secret（至少32字符），" +
                "例如: perm.jwt-secret=your-secret-key-at-least-32-characters-long!!"
            );
        }
    }
}
