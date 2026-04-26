package com.perm.starter.service;

import com.perm.common.entity.PermUser;
import com.perm.common.entity.PermUserRole;
import com.perm.common.exception.PermBizException;
import com.perm.common.util.PasswordEncoder;
import com.perm.starter.config.PermissionProperties;
import com.perm.starter.repository.PermUserRepository;
import com.perm.starter.repository.PermUserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 用户管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermUserService {

    private final PermUserRepository permUserRepository;
    private final PermUserRoleRepository permUserRoleRepository;
    private final PermAuthService permAuthService;
    private final PermissionProperties properties;

    /** 登录失败计数：username -> 失败次数 */
    private final ConcurrentHashMap<String, LoginFailInfo> loginFailMap = new ConcurrentHashMap<>();

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MS = 30 * 60 * 1000; // 30分钟锁定

    /**
     * 判断用户是否启用
     */
    public boolean isUserEnabled(Long userId) {
        return permUserRepository.findById(userId)
                .map(user -> Boolean.TRUE.equals(user.getEnabled()))
                .orElse(false);
    }

    /**
     * 用户登录
     */
    public PermUser login(String username, String password) {
        // 检查账户是否被临时锁定
        LoginFailInfo failInfo = loginFailMap.get(username);
        if (failInfo != null && failInfo.isLocked()) {
            throw new PermBizException(401, "账户已被临时锁定，请" + (LOCK_DURATION_MS / 60000) + "分钟后再试");
        }

        PermUser user = permUserRepository.findByUsername(username)
                .orElseThrow(() -> new PermBizException(401, "用户名或密码错误"));
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new PermBizException(401, "用户已被禁用");
        }
        if (!PasswordEncoder.matches(password, user.getPassword())) {
            recordLoginFail(username);
            throw new PermBizException(401, "用户名或密码错误");
        }

        // 登录成功，清除失败记录
        loginFailMap.remove(username);
        return user;
    }

    /**
     * 记录登录失败
     */
    private void recordLoginFail(String username) {
        loginFailMap.compute(username, (k, v) -> {
            if (v == null || v.isExpired()) return new LoginFailInfo(1);
            return new LoginFailInfo(v.count + 1);
        });
        LoginFailInfo info = loginFailMap.get(username);
        if (info.count >= MAX_LOGIN_ATTEMPTS) {
            log.warn("[PermStarter] 账户 {} 连续登录失败{}次，已临时锁定", username, info.count);
        }
    }

    /**
     * 获取所有用户
     */
    public List<PermUser> listAll() {
        return permUserRepository.findAll();
    }

    /**
     * 获取用户详情
     */
    public PermUser getById(Long id) {
        return permUserRepository.findById(id)
                .orElseThrow(() -> new PermBizException(404, "用户不存在: " + id));
    }

    /**
     * 创建用户
     */
    @Transactional
    public PermUser create(PermUser user) {
        if (permUserRepository.existsByUsername(user.getUsername())) {
            throw new PermBizException(400, "用户名已存在: " + user.getUsername());
        }
        user.setPassword(PasswordEncoder.encode(user.getPassword()));
        return permUserRepository.save(user);
    }

    /**
     * 更新用户
     */
    @Transactional
    public PermUser update(Long id, PermUser userUpdate) {
        PermUser user = getById(id);
        if (userUpdate.getNickname() != null) {
            user.setNickname(userUpdate.getNickname());
        }
        if (userUpdate.getPassword() != null && !userUpdate.getPassword().isEmpty()) {
            user.setPassword(PasswordEncoder.encode(userUpdate.getPassword()));
        }
        if (userUpdate.getEnabled() != null) {
            user.setEnabled(userUpdate.getEnabled());
        }
        return permUserRepository.save(user);
    }

    /**
     * 修改密码（需验证旧密码）
     */
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        PermUser user = getById(userId);
        if (!PasswordEncoder.matches(oldPassword, user.getPassword())) {
            throw new PermBizException(400, "旧密码不正确");
        }
        user.setPassword(PasswordEncoder.encode(newPassword));
        permUserRepository.save(user);
        permAuthService.clearUserPermissionCache(userId);
    }

    /**
     * 重置密码（管理员操作，无需旧密码）
     */
    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        PermUser user = getById(userId);
        user.setPassword(PasswordEncoder.encode(newPassword));
        permUserRepository.save(user);
        permAuthService.clearUserPermissionCache(userId);
    }

    /**
     * 删除用户
     */
    @Transactional
    public void delete(Long id) {
        permUserRoleRepository.deleteByUserId(id);
        permUserRepository.deleteById(id);
        permAuthService.clearUserPermissionCache(id);
    }

    /**
     * 获取用户角色ID列表
     */
    public List<Long> getUserRoleIds(Long userId) {
        return permUserRoleRepository.findByUserId(userId).stream()
                .map(PermUserRole::getRoleId)
                .collect(Collectors.toList());
    }

    /**
     * 分配用户角色（校验角色ID存在性）
     */
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        if (roleIds == null) roleIds = List.of();
        // 不做角色ID校验，由外层PermRoleService负责
        permUserRoleRepository.deleteByUserId(userId);
        if (!roleIds.isEmpty()) {
            List<PermUserRole> newRelations = roleIds.stream()
                    .map(roleId -> PermUserRole.builder()
                            .userId(userId)
                            .roleId(roleId)
                            .build())
                    .collect(Collectors.toList());
            permUserRoleRepository.saveAll(newRelations);
        }
        permAuthService.clearUserPermissionCache(userId);
    }

    /**
     * 初始化默认管理员（首次启动时调用）
     */
    @Transactional
    public String initDefaultAdmin() {
        if (!permUserRepository.existsByUsername("admin")) {
            // 使用配置的密码或生成随机密码
            String password = properties.getAdminPassword();
            boolean isRandom = false;
            if (password == null || password.isEmpty()) {
                password = generateRandomPassword();
                isRandom = true;
            }
            PermUser admin = PermUser.builder()
                    .username("admin")
                    .password(PasswordEncoder.encode(password))
                    .nickname("超级管理员")
                    .enabled(true)
                    .build();
            permUserRepository.save(admin);
            return isRandom ? password : null;
        }
        return null;
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
        StringBuilder sb = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 16; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * 登录失败信息
     */
    private static class LoginFailInfo {
        final int count;
        final long lockTime;

        LoginFailInfo(int count) {
            this.count = count;
            this.lockTime = System.currentTimeMillis();
        }

        boolean isLocked() {
            return count >= MAX_LOGIN_ATTEMPTS && !isExpired();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - lockTime > LOCK_DURATION_MS;
        }
    }
}
