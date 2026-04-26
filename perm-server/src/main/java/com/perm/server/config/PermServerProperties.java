package com.perm.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "perm")
public class PermServerProperties {

    private boolean enabled = true;
    private String jwtSecret;
    private long jwtExpiration = 86400000L;
    private String adminPassword;
    private boolean initAdmin = true;
    private boolean autoScan = false;
    private List<String> allowedOrigins = new ArrayList<>();

    public void validateJwtSecret() {
        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalStateException(
                "[PermServer] 必须配置 perm.jwt-secret（至少32字符）");
        }
    }
}
