package com.perm.server.service;

import com.perm.common.entity.PermRoleResource;
import com.perm.common.entity.PermUserRole;
import com.perm.common.util.PasswordEncoder;
import com.perm.server.config.PermServerProperties;
import com.perm.server.repository.PermResourceRepository;
import com.perm.server.repository.PermRoleResourceRepository;
import com.perm.server.repository.PermUserRoleRepository;
import com.perm.server.repository.PermUserRepository;
import com.perm.common.entity.PermUser;
import com.perm.common.exception.PermBizException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermServerAuthService {

    private final PermUserRepository permUserRepository;
    private final PermUserRoleRepository permUserRoleRepository;
    private final PermRoleResourceRepository permRoleResourceRepository;
    private final PermResourceRepository permResourceRepository;
    private final PermServerProperties properties;
    private final StringRedisTemplate redisTemplate;

    private static final String BLACKLIST_PREFIX = "perm:token:blacklist:";
    private static final String LOGIN_FAIL_PREFIX = "perm:login:fail:";

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }

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

    public Long parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String jti = claims.getId();
        if (jti != null && Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti))) {
            throw new PermBizException(401, "Token已失效");
        }
        return Long.parseLong(claims.getSubject());
    }

    public void invalidateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String jti = claims.getId();
            if (jti != null) {
                long ttl = claims.getExpiration().getTime() - System.currentTimeMillis();
                if (ttl > 0) {
                    redisTemplate.opsForValue().set(BLACKLIST_PREFIX + jti, "1", ttl, TimeUnit.MILLISECONDS);
                }
            }
        } catch (Exception ignored) {}
    }

    public PermUser login(String username, String password) {
        // 检查Redis中的登录失败次数
        String failKey = LOGIN_FAIL_PREFIX + username;
        String failCount = redisTemplate.opsForValue().get(failKey);
        if (failCount != null && Integer.parseInt(failCount) >= 5) {
            throw new PermBizException(401, "账户已被临时锁定，请30分钟后再试");
        }

        PermUser user = permUserRepository.findByUsername(username)
                .orElseThrow(() -> new PermBizException(401, "用户名或密码错误"));
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new PermBizException(401, "用户已被禁用");
        }
        if (!PasswordEncoder.matches(password, user.getPassword())) {
            // 记录失败次数到Redis
            redisTemplate.opsForValue().increment(failKey);
            redisTemplate.expire(failKey, 30, TimeUnit.MINUTES);
            throw new PermBizException(401, "用户名或密码错误");
        }

        // 登录成功，清除失败记录
        redisTemplate.delete(failKey);
        return user;
    }

    /**
     * 获取用户被授权的所有接口路径（METHOD:PATH格式），支持按serviceId过滤
     */
    public Set<String> getUserPermittedPaths(Long userId, String serviceId) {
        List<PermUserRole> userRoles = permUserRoleRepository.findByUserId(userId);
        if (userRoles.isEmpty()) return Collections.emptySet();

        List<Long> roleIds = userRoles.stream().map(PermUserRole::getRoleId).collect(Collectors.toList());
        List<PermRoleResource> roleResources = permRoleResourceRepository.findByRoleIds(roleIds);
        if (roleResources.isEmpty()) return Collections.emptySet();

        List<Long> resourceIds = roleResources.stream().map(PermRoleResource::getResourceId).collect(Collectors.toList());

        return permResourceRepository.findAllById(resourceIds).stream()
                .filter(r -> Boolean.TRUE.equals(r.getEnabled()))
                .filter(r -> serviceId == null || serviceId.equals(r.getServiceId()))
                .map(r -> r.getMethod().toUpperCase() + ":" + r.getPath())
                .collect(Collectors.toSet());
    }

    public void clearUserCache(Long userId) {
        // Redis模式下权限查询直接走DB，无需手动清缓存
        log.debug("[PermServer] 用户权限已刷新: userId={}", userId);
    }

    public void clearAllCache() {
        log.info("[PermServer] 全局权限缓存已刷新");
    }
}
