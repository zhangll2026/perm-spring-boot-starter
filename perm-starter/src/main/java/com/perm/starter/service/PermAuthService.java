package com.perm.starter.service;

import com.perm.common.entity.PermRoleResource;
import com.perm.common.entity.PermUserRole;
import com.perm.starter.config.PermissionProperties;
import com.perm.starter.repository.PermResourceRepository;
import com.perm.starter.repository.PermRoleResourceRepository;
import com.perm.starter.repository.PermUserRoleRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 权限认证服务 - Token管理、权限校验
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermAuthService {

    private final PermUserRoleRepository permUserRoleRepository;
    private final PermRoleResourceRepository permRoleResourceRepository;
    private final PermResourceRepository permResourceRepository;
    private final PermissionProperties properties;

    /** 权限缓存：userId -> METHOD:PATH集合 */
    private final Map<Long, Set<String>> permissionCache = new ConcurrentHashMap<>();

    /** Token黑名单：被注销的Token的jti集合 */
    private final Set<String> tokenBlacklist = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /** 缓存版本号，AtomicLong保证原子性 */
    private final AtomicLong cacheVersion = new AtomicLong(0);
    private final Map<Long, Long> userCacheVersion = new ConcurrentHashMap<>();

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成JWT Token
     */
    public String generateToken(Long userId, String username) {
        String jti = UUID.randomUUID().toString();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("jti", jti)
                .id(jti)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + properties.getJwtExpiration()))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析Token获取用户ID
     */
    public Long parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // 检查Token是否在黑名单中
        String jti = claims.getId();
        if (jti != null && tokenBlacklist.contains(jti)) {
            throw new RuntimeException("Token已失效");
        }

        return Long.parseLong(claims.getSubject());
    }

    /**
     * 将Token加入黑名单（登出时调用）
     */
    public void invalidateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String jti = claims.getId();
            if (jti != null) {
                tokenBlacklist.add(jti);
                log.debug("[PermStarter] Token已加入黑名单: jti={}", jti);
            }
        } catch (Exception e) {
            // Token已无效，无需处理
        }
    }

    /**
     * 获取用户被授权的所有接口路径（METHOD:PATH格式）
     */
    public Set<String> getUserPermittedPaths(Long userId) {
        long currentVersion = cacheVersion.get();
        Long userVersion = userCacheVersion.get(userId);

        // 版本不一致或无缓存，则刷新
        if (userVersion == null || userVersion < currentVersion || !permissionCache.containsKey(userId)) {
            refreshUserPermission(userId, currentVersion);
        }
        return permissionCache.getOrDefault(userId, Collections.emptySet());
    }

    /**
     * 刷新指定用户的权限缓存
     */
    private void refreshUserPermission(Long userId, long version) {
        List<PermUserRole> userRoles = permUserRoleRepository.findByUserId(userId);
        if (userRoles.isEmpty()) {
            permissionCache.put(userId, Collections.emptySet());
            userCacheVersion.put(userId, version);
            return;
        }

        List<Long> roleIds = userRoles.stream().map(PermUserRole::getRoleId).collect(Collectors.toList());
        List<PermRoleResource> roleResources = permRoleResourceRepository.findByRoleIds(roleIds);
        if (roleResources.isEmpty()) {
            permissionCache.put(userId, Collections.emptySet());
            userCacheVersion.put(userId, version);
            return;
        }

        List<Long> resourceIds = roleResources.stream().map(PermRoleResource::getResourceId).collect(Collectors.toList());
        Set<String> paths = permResourceRepository.findAllById(resourceIds).stream()
                .filter(r -> Boolean.TRUE.equals(r.getEnabled()))
                .map(r -> r.getMethod().toUpperCase() + ":" + r.getPath())
                .collect(Collectors.toSet());

        permissionCache.put(userId, paths);
        userCacheVersion.put(userId, version);
        log.debug("[PermStarter] 刷新用户权限缓存: userId={}, paths={}", userId, paths.size());
    }

    /**
     * 清除所有权限缓存（角色-资源关系变更时调用）
     */
    public void clearAllPermissionCache() {
        cacheVersion.incrementAndGet();
        // 不清空permissionCache，让各用户下次访问时懒加载刷新
        log.info("[PermStarter] 权限缓存版本已递增，将触发懒刷新");
    }

    /**
     * 清除指定用户的权限缓存
     */
    public void clearUserPermissionCache(Long userId) {
        permissionCache.remove(userId);
        userCacheVersion.remove(userId);
    }
}
