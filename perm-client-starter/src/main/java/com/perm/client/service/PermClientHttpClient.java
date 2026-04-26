package com.perm.client.service;

import com.perm.client.config.PermClientProperties;
import com.perm.common.dto.Result;
import com.perm.common.entity.PermResource;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;

/**
 * 客户端HTTP工具 - 调用perm-server的远程接口
 */
@Slf4j
public class PermClientHttpClient {

    private final PermClientProperties properties;
    private final ObjectMapper objectMapper;

    public PermClientHttpClient(PermClientProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    /**
     * 从perm-server获取用户在某服务的权限路径
     */
    @SuppressWarnings("unchecked")
    public Set<String> fetchUserPermissions(Long userId, String serviceId) {
        String url = properties.getServerUrl() + "/perm-api/auth/permissions?userId=" + userId;
        if (serviceId != null && !serviceId.isEmpty()) {
            url += "&serviceId=" + serviceId;
        }

        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(5))
                    .build();

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .header("X-Internal-Call", "true")
                    .timeout(java.time.Duration.ofSeconds(10))
                    .GET()
                    .build();

            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Result<?> result = objectMapper.readValue(response.body(), Result.class);
                if (result.getCode() == 200 && result.getData() != null) {
                    // Result的data是Set<String>，需要二次序列化转换
                    String dataJson = objectMapper.writeValueAsString(result.getData());
                    return objectMapper.readValue(dataJson, new TypeReference<Set<String>>() {});
                }
            }
            throw new RuntimeException("HTTP " + response.statusCode());
        } catch (Exception e) {
            throw new RuntimeException("调用perm-server权限接口失败: " + e.getMessage(), e);
        }
    }

    /**
     * 向perm-server注册本服务的接口资源
     */
    public void registerResources(String serviceId, List<PermResource> resources) {
        String url = properties.getServerUrl() + "/perm-api/resources/register?serviceId=" + serviceId;

        try {
            String body = objectMapper.writeValueAsString(resources);

            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(5))
                    .build();

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("X-Internal-Call", "true")
                    .timeout(java.time.Duration.ofSeconds(30))
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                    .build();

            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.info("[PermClient] 接口注册成功: serviceId={}, count={}", serviceId, resources.size());
            } else {
                log.warn("[PermClient] 接口注册失败: serviceId={}, status={}, body={}", serviceId, response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("[PermClient] 接口注册异常: serviceId={}, error={}", serviceId, e.getMessage());
        }
    }

    /**
     * 校验Token（调用perm-server的verify接口作为备选）
     */
    public Long verifyTokenRemote(String token) {
        String url = properties.getServerUrl() + "/perm-api/auth/verify";

        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(5))
                    .build();

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .header("X-Internal-Call", "true")
                    .timeout(java.time.Duration.ofSeconds(10))
                    .GET()
                    .build();

            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Result<?> result = objectMapper.readValue(response.body(), Result.class);
                if (result.getCode() == 200 && result.getData() != null) {
                    String dataJson = objectMapper.writeValueAsString(result.getData());
                    return objectMapper.readValue(dataJson, Long.class);
                }
            }
            return null;
        } catch (Exception e) {
            log.debug("[PermClient] 远程Token校验失败: {}", e.getMessage());
            return null;
        }
    }
}
