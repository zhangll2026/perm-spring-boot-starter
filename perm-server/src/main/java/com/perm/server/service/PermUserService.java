package com.perm.server.service;

import com.perm.common.entity.PermUser;
import com.perm.common.entity.PermUserRole;
import com.perm.common.exception.PermBizException;
import com.perm.common.util.PasswordEncoder;
import com.perm.server.config.PermServerProperties;
import com.perm.server.repository.PermUserRepository;
import com.perm.server.repository.PermUserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermUserService {

    private final PermUserRepository permUserRepository;
    private final PermUserRoleRepository permUserRoleRepository;
    private final PermServerAuthService authService;
    private final PermServerProperties properties;

    public PermUser getById(Long id) {
        return permUserRepository.findById(id)
                .orElseThrow(() -> new PermBizException(404, "用户不存在: " + id));
    }

    public List<PermUser> listAll() { return permUserRepository.findAll(); }

    @Transactional
    public PermUser create(PermUser user) {
        if (permUserRepository.existsByUsername(user.getUsername())) {
            throw new PermBizException(400, "用户名已存在: " + user.getUsername());
        }
        user.setPassword(PasswordEncoder.encode(user.getPassword()));
        return permUserRepository.save(user);
    }

    @Transactional
    public PermUser update(Long id, PermUser userUpdate) {
        PermUser user = getById(id);
        if (userUpdate.getNickname() != null) user.setNickname(userUpdate.getNickname());
        if (userUpdate.getPassword() != null && !userUpdate.getPassword().isEmpty()) {
            user.setPassword(PasswordEncoder.encode(userUpdate.getPassword()));
        }
        if (userUpdate.getEnabled() != null) user.setEnabled(userUpdate.getEnabled());
        return permUserRepository.save(user);
    }

    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        PermUser user = getById(userId);
        if (!PasswordEncoder.matches(oldPassword, user.getPassword())) {
            throw new PermBizException(400, "旧密码不正确");
        }
        user.setPassword(PasswordEncoder.encode(newPassword));
        permUserRepository.save(user);
    }

    @Transactional
    public void delete(Long id) {
        permUserRoleRepository.deleteByUserId(id);
        permUserRepository.deleteById(id);
    }

    public List<Long> getUserRoleIds(Long userId) {
        return permUserRoleRepository.findByUserId(userId).stream()
                .map(PermUserRole::getRoleId).collect(Collectors.toList());
    }

    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        permUserRoleRepository.deleteByUserId(userId);
        if (roleIds != null && !roleIds.isEmpty()) {
            List<PermUserRole> list = roleIds.stream()
                    .map(rid -> PermUserRole.builder().userId(userId).roleId(rid).build())
                    .collect(Collectors.toList());
            permUserRoleRepository.saveAll(list);
        }
        authService.clearUserCache(userId);
    }

    @Transactional
    public String initDefaultAdmin() {
        if (!permUserRepository.existsByUsername("admin")) {
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

    public boolean isUserEnabled(Long userId) {
        return permUserRepository.findById(userId)
                .map(u -> Boolean.TRUE.equals(u.getEnabled()))
                .orElse(false);
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
        StringBuilder sb = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 16; i++) sb.append(chars.charAt(random.nextInt(chars.length())));
        return sb.toString();
    }
}
