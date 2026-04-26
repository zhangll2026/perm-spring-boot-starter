package com.perm.starter.interceptor;

import com.perm.starter.config.PermissionProperties;
import com.perm.starter.context.UserContext;
import com.perm.starter.service.PermAuthService;
import com.perm.starter.service.PermUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * 业务接口权限拦截器 - 根据用户角色判断是否有权访问
 */
@Slf4j
@RequiredArgsConstructor
public class PermissionInterceptor implements HandlerInterceptor {

    private final PermAuthService permAuthService;
    private final PermUserService permUserService;
    private final PermissionProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestPath = request.getRequestURI();
        String method = request.getMethod();

        // OPTIONS请求直接放行
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        // 1. 检查是否为放行路径
        if (isExcludedPath(requestPath)) {
            return true;
        }

        // 2. 获取Token
        String token = extractToken(request);
        if (!StringUtils.hasText(token)) {
            if (properties.isAllowAnonymous()) {
                return true;
            }
            writeError(response, 403, "未登录，请先登录");
            return false;
        }

        // 3. 解析Token
        Long userId;
        try {
            userId = permAuthService.parseToken(token);
        } catch (Exception e) {
            writeError(response, 403, "Token无效或已过期");
            return false;
        }

        // 4. 设置用户上下文
        UserContext.setUserId(userId);

        // 5. 检查用户是否启用
        if (!permUserService.isUserEnabled(userId)) {
            writeError(response, 403, "用户已被禁用");
            return false;
        }

        // 6. 获取用户角色允许的所有接口路径
        Set<String> permittedPaths = permAuthService.getUserPermittedPaths(userId);
        if (permittedPaths == null || permittedPaths.isEmpty()) {
            writeError(response, 403, "无访问权限");
            return false;
        }

        // 7. 匹配当前请求
        String fullPath = method.toUpperCase() + ":" + requestPath;
        boolean hasPermission = permittedPaths.contains(fullPath) ||
                permittedPaths.stream().anyMatch(pattern -> pathMatcher.match(pattern, fullPath));

        if (!hasPermission) {
            log.debug("[PermStarter] 权限拒绝: user={}, path={}", userId, fullPath);
            writeError(response, 403, "无访问权限");
            return false;
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    /**
     * 判断是否为放行路径（使用配置项，不再硬编码）
     */
    private boolean isExcludedPath(String path) {
        // 权限管理自身的接口和页面始终放行（由PermApiAuthInterceptor单独鉴权）
        if (path.startsWith(properties.getApiPath() + "/") || path.startsWith(properties.getAdminPath())) {
            return true;
        }

        // 用户自定义的放行路径
        List<String> excludePaths = properties.getExcludePaths();
        if (excludePaths != null) {
            for (String excludePath : excludePaths) {
                if (pathMatcher.match(excludePath, path)) {
                    return true;
                }
            }
        }

        return false;
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return request.getHeader("X-Perm-Token");
    }

    /**
     * 返回错误响应（JSON安全转义）
     */
    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String json = "{\"code\":" + status + ",\"message\":\"" + escapeJson(message) + "\"}";
        response.getWriter().write(json);
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
