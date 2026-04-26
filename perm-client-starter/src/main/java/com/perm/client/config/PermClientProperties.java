package com.perm.client.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "perm")
public class PermClientProperties {

    /** 模式：client（轻量客户端） */
    private String mode = "client";

    /** 是否启用 */
    private boolean enabled = true;

    /** JWT密钥（与perm-server一致，或配RSA公钥） */
    private String jwtSecret;

    /** JWT过期时间 */
    private long jwtExpiration = 86400000L;

    /** 权限服务地址 */
    private String serverUrl = "http://localhost:9090";

    /** 当前服务ID（默认取spring.application.name） */
    private String serviceId;

    /** 信任网关传递的用户标识 */
    private boolean trustGateway = true;

    /** 网关传递用户ID的请求头 */
    private String userIdHeader = "X-User-Id";

    /** 网关传递用户名的请求头 */
    private String usernameHeader = "X-Username";

    /** 内部调用标识头 */
    private String internalHeader = "X-Internal-Call";

    /** 放行路径 */
    private List<String> excludePaths = new ArrayList<>();

    /** 管理API路径前缀 */
    private String apiPath = "/perm-api";

    /** 管理页面路径前缀 */
    private String adminPath = "/perm-admin";

    /** 权限缓存过期时间（秒） */
    private long cacheExpireSeconds = 300;

    public void validateJwtSecret() {
        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalStateException(
                "[PermClient] 必须配置 perm.jwt-secret（至少32字符）");
        }
    }
}
