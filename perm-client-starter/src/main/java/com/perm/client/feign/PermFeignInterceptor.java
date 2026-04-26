package com.perm.client.feign;

import com.perm.client.config.PermClientProperties;
import com.perm.client.context.UserContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Feign请求拦截器 - 传播用户上下文到下游服务
 * <p>
 * 在微服务调用链中，将当前请求的用户ID和用户名通过请求头传递给下游服务。
 * 下游服务通过InternalCallInterceptor检测到这些头后恢复UserContext。
 * <p>
 * 仅在Classpath中存在feign-core时自动注册
 */
@Slf4j
@RequiredArgsConstructor
public class PermFeignInterceptor implements RequestInterceptor {

    private final PermClientProperties properties;

    @Override
    public void apply(RequestTemplate template) {
        Long userId = UserContext.getUserId();
        String username = UserContext.getUsername();

        if (userId != null) {
            template.header(properties.getUserIdHeader(), String.valueOf(userId));
        }
        if (username != null) {
            template.header(properties.getUsernameHeader(), username);
        }

        // 标记为内部调用，下游服务跳过鉴权
        template.header(properties.getInternalHeader(), "true");

        log.debug("[PermClient] Feign传播用户上下文: userId={}, username={}", userId, username);
    }
}
