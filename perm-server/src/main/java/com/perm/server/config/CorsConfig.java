package com.perm.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {

    private final PermServerProperties properties;

    public CorsConfig(PermServerProperties properties) {
        this.properties = properties;
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = properties.getAllowedOrigins();
        if (origins != null && !origins.isEmpty()) {
            config.setAllowedOriginPatterns(origins);
        } else {
            config.setAllowedOriginPatterns(List.of("localhost"));
        }
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/perm-api/**", config);
        return new CorsFilter(source);
    }
}
