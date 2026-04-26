package com.perm.client.interceptor;

import com.perm.client.config.PermClientProperties;
import com.perm.client.context.UserContext;
import com.perm.client.service.PermClientAuthService;
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
 * 微服务客户端权限拦截器
 * <p>
 * 两种模式：
 * 1. trustGateway=true（默认）：信任网关传递的X-User-Id/X-Username头，不再验证JWT
 * 2. trustGateway=false：自行验证JWT Token获取用户身份
 * <p>
 * 获取到用户ID后，从perm-server远程查询权限，本地缓存后校验
 */
@Slf4j
@RequiredArgsConstructor
public class ClientPermissionInterceptor implements HandlerInterceptor {

    private final PermClientAuthService authService;
    private final PermClientProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestPath = request.getRequestURI();
        String method = request.getMethod();

        // OPTIONS请求直接放行
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        // 内部调用标记（由InternalCallInterceptor处理，这里直接放行）
        String internalFlag = request.getHeader(properties.getInternalHeader());
        if ("true".equalsIgnoreCase(internalFlag)) {
            return true;
        }

        // 放行路径
        if (isExcludedPath(requestPath)) {
            return true;
        }

        // 获取用户ID
        Long userId = resolveUserId(request);
        if (userId == null) {
            writeError(response, 403, "未登录，请先登录");
            return false;
        }

        // 设置用户上下文
        UserContext.setUserId(userId);
        String username = resolveUsername(request);
        if (username != null) {
            UserContext.setUsername(username);
        }

        // 获取用户权限路径
        Set<String> permittedPaths = authService.getUserPermittedPaths(userId);
        if (permittedPaths == null || permittedPaths.isEmpty()) {
            writeError(response, 403, "无访问权限");
            return false;
        }

        // 匹配当前请求
        String fullPath = method.toUpperCase() + ":" + requestPath;
        boolean hasPermission = permittedPaths.contains(fullPath) ||
                permittedPaths.stream().anyMatch(pattern -> pathMatcher.match(pattern, fullPath));

        if (!hasPermission) {
            log.debug("[PermClient] 权限拒绝: user={}, path={}", userId, fullPath);
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
     * 解析用户ID：优先从网关头读取，其次从JWT解析
     */
    private Long resolveUserId(HttpServletRequest request) {
        // 模式1：信任网关
        if (properties.isTrustGateway()) {
            String userIdStr = request.getHeader(properties.getUserIdHeader());
            if (StringUtils.hasText(userIdStr)) {
                try {
                    return Long.parseLong(userIdStr);
                } catch (NumberFormatException e) {
                    log.warn("[PermClient] 无效的用户ID头: {}", userIdStr);
                }
            }
        }

        // 模式2：自行验证JWT
        String token = extractToken(request);
        if (StringUtils.hasText(token)) {
            try {
                return authService.verifyToken(token);
            } catch (Exception e) {
                log.debug("[PermClient] Token验证失败: {}", e.getMessage());
            }
        }

        return null;
    }

    /**
     * 解析用户名
     */
    private String resolveUsername(HttpServletRequest request) {
        if (properties.isTrustGateway()) {
            String username = request.getHeader(properties.getUsernameHeader());
            if (StringUtils.hasText(username)) {
                return username;
            }
        }

        String token = extractToken(request);
        if (StringUtils.hasText(token)) {
            try {
                return authService.getUsernameFromToken(token);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private boolean isExcludedPath(String path) {
        // perm管理路径放行
        if (path.startsWith(properties.getApiPath() + "/") || path.startsWith(properties.getAdminPath())) {
            return true;
        }

        // 用户自定义放行路径
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
