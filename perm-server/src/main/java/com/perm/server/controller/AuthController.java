package com.perm.server.controller;

import com.perm.common.dto.*;
import com.perm.common.entity.PermUser;
import com.perm.server.service.PermServerAuthService;
import com.perm.server.service.PermUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/perm-api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final PermUserService permUserService;
    private final PermServerAuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        if (!StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
            return Result.error(400, "用户名和密码不能为空");
        }
        PermUser user = authService.login(request.getUsername(), request.getPassword());
        String token = authService.generateToken(user.getId(), user.getUsername());
        return Result.success(LoginResponse.builder()
                .token(token).userId(user.getId())
                .username(user.getUsername()).nickname(user.getNickname()).build());
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = extractBearerToken(authHeader);
        if (token != null) authService.invalidateToken(token);
        return Result.success();
    }

    @GetMapping("/info")
    public Result<UserDTO> info(@RequestHeader("Authorization") String authHeader) {
        String token = extractBearerToken(authHeader);
        if (token == null) return Result.error(401, "Token无效");
        Long userId = authService.parseToken(token);
        PermUser user = permUserService.getById(userId);
        return Result.success(UserDTO.builder()
                .id(user.getId()).username(user.getUsername())
                .nickname(user.getNickname()).enabled(user.getEnabled())
                .createdAt(user.getCreatedAt()).updatedAt(user.getUpdatedAt()).build());
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@RequestHeader("Authorization") String authHeader,
                                       @RequestBody ChangePasswordRequest request) {
        String token = extractBearerToken(authHeader);
        if (token == null) return Result.error(401, "Token无效");
        Long userId = authService.parseToken(token);
        permUserService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return Result.success();
    }

    /** 校验Token并返回用户ID（供客户端调用） */
    @GetMapping("/verify")
    public Result<Long> verify(@RequestHeader("Authorization") String authHeader) {
        String token = extractBearerToken(authHeader);
        if (token == null) return Result.error(401, "Token无效");
        Long userId = authService.parseToken(token);
        return Result.success(userId);
    }

    /** 查询用户在某服务的权限路径（供客户端调用） */
    @GetMapping("/permissions")
    public Result<java.util.Set<String>> getPermissions(@RequestParam Long userId,
                                                         @RequestParam(required = false) String serviceId) {
        java.util.Set<String> paths = authService.getUserPermittedPaths(userId, serviceId);
        return Result.success(paths);
    }

    private String extractBearerToken(String authHeader) {
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
