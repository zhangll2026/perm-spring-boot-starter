package com.perm.starter.controller;

import com.perm.common.dto.*;
import com.perm.common.entity.PermUser;
import com.perm.common.exception.PermBizException;
import com.perm.starter.service.PermAuthService;
import com.perm.starter.service.PermUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/perm-api/auth")
@RequiredArgsConstructor
public class PermAuthController {

    private final PermUserService permUserService;
    private final PermAuthService permAuthService;

    /**
     * 登录
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        if (!StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
            return Result.error(400, "用户名和密码不能为空");
        }
        PermUser user = permUserService.login(request.getUsername(), request.getPassword());
        String token = permAuthService.generateToken(user.getId(), user.getUsername());
        LoginResponse response = LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .build();
        return Result.success(response);
    }

    /**
     * 登出（将Token加入黑名单）
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = extractBearerToken(authHeader);
        if (token != null) {
            permAuthService.invalidateToken(token);
        }
        return Result.success();
    }

    /**
     * 获取当前用户信息（不返回密码）
     */
    @GetMapping("/info")
    public Result<UserDTO> info(@RequestHeader("Authorization") String authHeader) {
        String token = extractBearerToken(authHeader);
        if (token == null) {
            return Result.error(401, "Token无效");
        }
        Long userId = permAuthService.parseToken(token);
        PermUser user = permUserService.getById(userId);
        UserDTO dto = UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
        return Result.success(dto);
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestHeader("Authorization") String authHeader,
                                       @RequestBody ChangePasswordRequest request) {
        String token = extractBearerToken(authHeader);
        if (token == null) {
            return Result.error(401, "Token无效");
        }
        Long userId = permAuthService.parseToken(token);
        permUserService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return Result.success();
    }

    private String extractBearerToken(String authHeader) {
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
