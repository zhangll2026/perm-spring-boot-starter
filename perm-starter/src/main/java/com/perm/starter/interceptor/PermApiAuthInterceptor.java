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
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 管理API鉴权拦截器 - perm-api接口需要Token认证
 * <p>
 * 只拦截 /perm-api/ 下除登录接口外的请求，要求有效Token
 */
@Slf4j
@RequiredArgsConstructor
public class PermApiAuthInterceptor implements HandlerInterceptor {

    private final PermAuthService permAuthService;
    private final PermUserService permUserService;
    private final PermissionProperties properties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestPath = request.getRequestURI();
        String method = request.getMethod();

        // OPTIONS请求直接放行（CORS预检）
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        // 登录接口放行
        String loginPath = properties.getApiPath() + "/auth/login";
        if (requestPath.equals(loginPath)) {
            return true;
        }

        // 获取Token
        String token = extractToken(request);
        if (!StringUtils.hasText(token)) {
            writeError(response, 401, "未登录，请先登录");
            return false;
        }

        // 解析Token
        Long userId;
        try {
            userId = permAuthService.parseToken(token);
        } catch (Exception e) {
            writeError(response, 401, "Token无效或已过期");
            return false;
        }

        // 检查用户是否启用
        if (!permUserService.isUserEnabled(userId)) {
            writeError(response, 403, "用户已被禁用");
            return false;
        }

        // 设置用户上下文
        UserContext.setUserId(userId);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
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
