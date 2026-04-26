package com.perm.client.config;

import com.perm.client.feign.PermFeignInterceptor;
import com.perm.client.interceptor.ClientPermissionInterceptor;
import com.perm.client.interceptor.InternalCallInterceptor;
import com.perm.client.scanner.RemoteApiScanner;
import com.perm.client.service.PermClientAuthService;
import com.perm.client.service.PermClientHttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 微服务客户端自动配置
 * <p>
 * 引入perm-client-starter后自动生效，可通过 perm.enabled=false 关闭
 * <p>
 * 适用场景：微服务架构中的各业务服务，配合perm-server使用
 * <p>
 * 核心流程：
 * 1. 启动时扫描本地接口，远程注册到perm-server
 * 2. 请求进入时，从网关头/JWT获取用户身份
 * 3. 从perm-server远程查询用户权限（本地缓存）
 * 4. 校验当前请求是否在权限范围内
 * 5. Feign调用时自动传播用户上下文
 */
@Slf4j
@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
@ConditionalOnProperty(prefix = "perm", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(PermClientProperties.class)
@Import({
        PermClientAuthService.class,
        PermClientHttpClient.class
})
public class PermClientAutoConfiguration implements WebMvcConfigurer {

    private final PermClientProperties properties;
    private final PermClientAuthService authService;
    private final PermClientHttpClient httpClient;
    private final Environment environment;

    public PermClientAutoConfiguration(PermClientProperties properties,
                                       PermClientAuthService authService,
                                       PermClientHttpClient httpClient,
                                       Environment environment) {
        this.properties = properties;
        this.authService = authService;
        this.httpClient = httpClient;
        this.environment = environment;
    }

    /**
     * 注册拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. 内部调用拦截器（最高优先级，order=-1）
        InternalCallInterceptor internalInterceptor = new InternalCallInterceptor(properties);
        registry.addInterceptor(internalInterceptor)
                .addPathPatterns("/**")
                .order(-1);

        // 2. 业务权限拦截器
        ClientPermissionInterceptor permInterceptor = new ClientPermissionInterceptor(authService, properties);
        registry.addInterceptor(permInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        properties.getAdminPath() + "/**",
                        properties.getApiPath() + "/**",
                        "/error"
                )
                .order(0);

        log.info("[PermClient] 权限拦截器已注册");
    }

    /**
     * Feign拦截器 - 仅在Classpath中有Feign时注册
     */
    @Bean
    @ConditionalOnClass(name = "feign.RequestInterceptor")
    public PermFeignInterceptor permFeignInterceptor() {
        log.info("[PermClient] Feign上下文传播拦截器已注册");
        return new PermFeignInterceptor(properties);
    }

    /**
     * 启动时初始化
     */
    @Bean
    public ApplicationRunner permClientInitializer() {
        return args -> {
            // 校验JWT密钥
            properties.validateJwtSecret();

            // 如果serviceId未配置，取spring.application.name
            if (properties.getServiceId() == null || properties.getServiceId().isEmpty()) {
                String appName = environment.getProperty("spring.application.name");
                if (appName != null && !appName.isEmpty()) {
                    properties.setServiceId(appName);
                    log.info("[PermClient] 自动设置serviceId: {}", appName);
                } else {
                    log.warn("[PermClient] 未配置perm.service-id且spring.application.name为空，接口注册将跳过");
                }
            }

            // 扫描本地接口并注册到perm-server
            try {
                RemoteApiScanner scanner = new RemoteApiScanner(
                        applicationContext.getBean(org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping.class),
                        properties, httpClient);
                scanner.scanAndRegister();
            } catch (Exception e) {
                log.warn("[PermClient] 接口注册失败: {}", e.getMessage());
            }

            log.info("==============================================");
            log.info("[PermClient] 权限客户端初始化完成");
            log.info("[PermClient] 权限服务: {}", properties.getServerUrl());
            log.info("[PermClient] 服务ID: {}", properties.getServiceId());
            log.info("[PermClient] 信任网关: {}", properties.isTrustGateway());
            log.info("==============================================");
        };
    }

    private org.springframework.context.ApplicationContext applicationContext;

    @org.springframework.beans.factory.annotation.Autowired
    public void setApplicationContext(org.springframework.context.ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}
