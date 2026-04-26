package com.perm.client.interceptor;

import com.perm.client.config.PermClientProperties;
import com.perm.client.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 内部调用拦截器
 * <p>
 * 检测X-Internal-Call请求头，标识微服务间的内部调用。
 * 内部调用时：
 * - 从X-User-Id/X-Username头恢复用户上下文
 * - 跳过权限校验（由调用方已鉴权）
 * <p>
 * 此拦截器需在ClientPermissionInterceptor之前执行（order更小）
 */
@Slf4j
@RequiredArgsConstructor
public class InternalCallInterceptor implements HandlerInterceptor {

    private final PermClientProperties properties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String internalFlag = request.getHeader(properties.getInternalHeader());
        if ("true".equalsIgnoreCase(internalFlag)) {
            // 内部调用，恢复用户上下文
            String userIdStr = request.getHeader(properties.getUserIdHeader());
            if (StringUtils.hasText(userIdStr)) {
                try {
                    UserContext.setUserId(Long.parseLong(userIdStr));
                } catch (NumberFormatException e) {
                    log.warn("[PermClient] 内部调用用户ID无效: {}", userIdStr);
                }
            }
            String username = request.getHeader(properties.getUsernameHeader());
            if (StringUtils.hasText(username)) {
                UserContext.setUsername(username);
            }
            log.debug("[PermClient] 内部调用: userId={}, username={}", userIdStr, username);
            // 直接放行，后续ClientPermissionInterceptor会检测internalFlag跳过鉴权
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 仅在内部调用时清理，其他情况由ClientPermissionInterceptor清理
        String internalFlag = request.getHeader(properties.getInternalHeader());
        if ("true".equalsIgnoreCase(internalFlag)) {
            UserContext.clear();
        }
    }
}
