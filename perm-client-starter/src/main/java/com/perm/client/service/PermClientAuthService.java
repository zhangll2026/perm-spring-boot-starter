package com.perm.client.service;

import com.perm.client.config.PermClientProperties;
import com.perm.common.dto.Result;
import com.perm.common.entity.PermResource;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 客户端权限认证服务
 * <p>
 * 职责：JWT本地验证 + 远程权限查询（从perm-server获取）
 */
@Slf4j
public class PermClientAuthService {

    private final PermClientProperties properties;
    private final PermClientHttpClient httpClient;

    /** 权限缓存：userId -> METHOD:PATH集合 */
    private final Map<Long, Set<String>> permissionCache = new ConcurrentHashMap<>();

    /** 缓存时间戳：userId -> 上次刷新时间ms */
    private final Map<Long, Long> cacheTimestamp = new ConcurrentHashMap<>();

    /** 缓存版本号（角色变更时远程通知递增） */
    private final AtomicLong cacheVersion = new AtomicLong(0);
    private final Map<Long, Long> userCacheVersion = new ConcurrentHashMap<>();

    public PermClientAuthService(PermClientProperties properties, PermClientHttpClient httpClient) {
        this.properties = properties;
        this.httpClient = httpClient;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 验证JWT Token并返回用户ID
     */
    public Long verifyToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 从Token中提取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("username", String.class);
    }

    /**
     * 获取用户在本服务的被授权接口路径（METHOD:PATH格式）
     * <p>
     * 优先使用本地缓存，缓存过期后从perm-server远程获取
     */
    public Set<String> getUserPermittedPaths(Long userId) {
        long now = System.currentTimeMillis();
        long expireMs = properties.getCacheExpireSeconds() * 1000L;

        Long lastRefresh = cacheTimestamp.get(userId);
        Long userVersion = userCacheVersion.get(userId);
        long currentVersion = cacheVersion.get();

        // 缓存有效且版本一致，直接返回
        boolean cacheValid = lastRefresh != null
                && (now - lastRefresh) < expireMs
                && userVersion != null
                && userVersion >= currentVersion;

        if (cacheValid && permissionCache.containsKey(userId)) {
            return permissionCache.get(userId);
        }

        // 远程获取
        refreshFromServer(userId);
        return permissionCache.getOrDefault(userId, Collections.emptySet());
    }

    /**
     * 从perm-server远程刷新用户权限
     */
    private void refreshFromServer(Long userId) {
        try {
            Set<String> paths = httpClient.fetchUserPermissions(userId, properties.getServiceId());
            permissionCache.put(userId, paths);
            cacheTimestamp.put(userId, System.currentTimeMillis());
            userCacheVersion.put(userId, cacheVersion.get());
            log.debug("[PermClient] 从perm-server刷新权限: userId={}, paths={}", userId, paths.size());
        } catch (Exception e) {
            log.warn("[PermClient] 从perm-server获取权限失败，使用旧缓存: userId={}, error={}", userId, e.getMessage());
            // 远程调用失败时，保留旧缓存不清理
        }
    }

    /**
     * 清除指定用户缓存
     */
    public void clearUserCache(Long userId) {
        permissionCache.remove(userId);
        cacheTimestamp.remove(userId);
        userCacheVersion.remove(userId);
    }

    /**
     * 清除全部缓存
     */
    public void clearAllCache() {
        cacheVersion.incrementAndGet();
        log.info("[PermClient] 权限缓存版本已递增，将触发懒刷新");
    }
}
