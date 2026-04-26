package com.perm.starter.config;

import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * CORS配置 - 根据用户配置限制允许的来源
 */
@RequiredArgsConstructor
public class PermCorsConfig implements WebMvcConfigurer {

    private final PermissionProperties properties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = properties.getAllowedOrigins();
        registry.addMapping(properties.getApiPath() + "/**")
                .allowedOriginPatterns(origins != null && !origins.isEmpty()
                        ? origins.toArray(new String[0])
                        : new String[]{"localhost"})
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
