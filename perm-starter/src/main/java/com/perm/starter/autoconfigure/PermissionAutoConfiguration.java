package com.perm.starter.autoconfigure;

import com.perm.starter.config.GlobalExceptionHandler;
import com.perm.starter.config.PermCorsConfig;
import com.perm.starter.config.PermissionProperties;
import com.perm.starter.controller.PermAdminController;
import com.perm.starter.controller.PermAuthController;
import com.perm.starter.controller.PermResourceController;
import com.perm.starter.controller.PermRoleController;
import com.perm.starter.controller.PermUserController;
import com.perm.starter.interceptor.PermApiAuthInterceptor;
import com.perm.starter.interceptor.PermissionInterceptor;
import com.perm.starter.scanner.ApiScanner;
import com.perm.starter.service.PermAuthService;
import com.perm.starter.service.PermResourceService;
import com.perm.starter.service.PermRoleService;
import com.perm.starter.service.PermUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 权限管理自动配置类
 * <p>
 * 引入jar包后自动生效，可通过 perm.enabled=false 关闭
 */
@Slf4j
@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnClass(name = {
        "org.springframework.web.servlet.DispatcherServlet",
        "jakarta.persistence.EntityManager"
})
@ConditionalOnProperty(prefix = "perm", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(PermissionProperties.class)
@EnableJpaRepositories(basePackages = "com.perm.starter.repository")
@EntityScan(basePackages = "com.perm.common.entity")
@Import({
        PermCorsConfig.class,
        GlobalExceptionHandler.class,
        PermAuthService.class,
        PermResourceService.class,
        PermRoleService.class,
        PermUserService.class,
        ApiScanner.class,
        PermAdminController.class,
        PermAuthController.class,
        PermResourceController.class,
        PermRoleController.class,
        PermUserController.class
})
@RequiredArgsConstructor
public class PermissionAutoConfiguration implements WebMvcConfigurer {

    private final PermissionProperties properties;
    private final PermAuthService permAuthService;
    private final PermUserService permUserService;

    /**
     * 注册拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. 管理API鉴权拦截器（拦截 /perm-api/**，需要Token）
        PermApiAuthInterceptor apiAuthInterceptor = new PermApiAuthInterceptor(
                permAuthService, permUserService, properties);
        registry.addInterceptor(apiAuthInterceptor)
                .addPathPatterns(properties.getApiPath() + "/**")
                .order(0);

        // 2. 业务接口权限拦截器（拦截所有非perm路径）
        PermissionInterceptor permInterceptor = new PermissionInterceptor(
                permAuthService, permUserService, properties);
        registry.addInterceptor(permInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        properties.getAdminPath() + "/**",
                        properties.getApiPath() + "/**",
                        "/error"
                )
                .order(1);

        log.info("[PermStarter] 权限拦截器已注册");
    }

    /**
     * 启动完成后执行初始化
     */
    @Bean
    public ApplicationRunner permissionInitializer(PermUserService userService, ApiScanner apiScanner, PermissionProperties props) {
        return args -> {
            // 校验JWT密钥配置
            props.validateJwtSecret();

            // 初始化默认管理员
            String randomPassword = null;
            if (props.isInitAdmin()) {
                try {
                    randomPassword = userService.initDefaultAdmin();
                } catch (Exception e) {
                    log.warn("[PermStarter] 初始化默认管理员失败: {}", e.getMessage());
                }
            }

            // 自动扫描接口
            if (props.isAutoScan()) {
                try {
                    apiScanner.scanAndPersist();
                } catch (Exception e) {
                    log.warn("[PermStarter] 自动扫描接口失败: {}", e.getMessage());
                }
            }

            log.info("==============================================");
            log.info("[PermStarter] 权限管理组件初始化完成");
            log.info("[PermStarter] 管理界面: {}{}", props.getAdminPath(), "/index.html");
            log.info("[PermStarter] 默认账号: admin");
            if (randomPassword != null) {
                log.info("[PermStarter] 随机密码: {} （请立即登录修改！）", randomPassword);
                log.info("[PermStarter] 也可通过 perm.admin-password 配置初始密码");
            } else {
                log.info("[PermStarter] 密码: 已通过 perm.admin-password 配置");
            }
            log.info("==============================================");
        };
    }
}
