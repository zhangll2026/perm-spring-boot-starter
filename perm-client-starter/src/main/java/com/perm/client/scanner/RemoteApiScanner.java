package com.perm.client.scanner;

import com.perm.client.config.PermClientProperties;
import com.perm.client.service.PermClientHttpClient;
import com.perm.common.entity.PermResource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 远程API接口扫描器
 * <p>
 * 扫描当前服务的Controller接口，然后通过HTTP注册到perm-server
 * 与单体版的ApiScanner不同，这里不操作本地数据库，而是远程推送
 */
@Slf4j
@RequiredArgsConstructor
public class RemoteApiScanner {

    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    private final PermClientProperties properties;
    private final PermClientHttpClient httpClient;

    /**
     * 扫描当前服务的Controller接口并远程注册到perm-server
     */
    public void scanAndRegister() {
        String serviceId = properties.getServiceId();
        if (serviceId == null || serviceId.isEmpty()) {
            log.warn("[PermClient] 未配置 perm.service-id，跳过接口注册");
            return;
        }

        log.info("[PermClient] 开始扫描API接口，serviceId={}...", serviceId);

        Map<RequestMappingInfo, HandlerMethod> handlerMethods = requestMappingHandlerMapping.getHandlerMethods();
        List<PermResource> resources = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int count = 0;

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            RequestMappingInfo mappingInfo = entry.getKey();
            HandlerMethod handlerMethod = entry.getValue();

            if (!isUserController(handlerMethod)) {
                continue;
            }

            Set<String> patterns = mappingInfo.getPatternValues();
            if (patterns.isEmpty()) continue;

            Set<String> httpMethods = mappingInfo.getMethodsCondition().getMethods()
                    .stream().map(m -> m.name()).collect(Collectors.toSet());
            if (httpMethods.isEmpty()) {
                httpMethods = Set.of("GET", "POST", "PUT", "DELETE", "PATCH");
            }

            String path = patterns.iterator().next();
            String controllerClass = handlerMethod.getBeanType().getSimpleName();
            String methodName = handlerMethod.getMethod().getName();
            String groupName = getGroupName(handlerMethod);
            String description = getDescription(handlerMethod);

            for (String httpMethod : httpMethods) {
                String key = httpMethod.toUpperCase() + ":" + path;
                if (seen.contains(key)) continue;
                seen.add(key);

                PermResource resource = PermResource.builder()
                        .path(path)
                        .method(httpMethod)
                        .description(description)
                        .controllerClass(controllerClass)
                        .methodName(methodName)
                        .groupName(groupName)
                        .serviceId(serviceId)
                        .enabled(true)
                        .build();
                resources.add(resource);
                count++;
            }
        }

        // 远程注册到perm-server
        if (!resources.isEmpty()) {
            httpClient.registerResources(serviceId, resources);
        }

        log.info("[PermClient] API接口扫描完成，serviceId={}，共 {} 个接口已注册到perm-server", serviceId, count);
    }

    private boolean isUserController(HandlerMethod handlerMethod) {
        Class<?> beanType = handlerMethod.getBeanType();
        Package pkg = beanType.getPackage();
        if (pkg == null) return true;
        String packageName = pkg.getName();
        if (packageName.startsWith("org.springframework.")) return false;
        if (packageName.startsWith("com.perm.client.")) return false;
        return true;
    }

    @SuppressWarnings("unchecked")
    private String getGroupName(HandlerMethod handlerMethod) {
        Class<?> beanType = handlerMethod.getBeanType();

        String swagger2Tag = readSwagger2ApiTag(beanType);
        if (swagger2Tag != null) return swagger2Tag;

        String swagger3Tag = readAnnotationProperty(
                beanType, "io.swagger.v3.oas.annotations.tags.Tag", "name", String.class);
        if (swagger3Tag != null) return swagger3Tag;

        String name = beanType.getSimpleName();
        if (name.endsWith("Controller")) {
            name = name.substring(0, name.length() - "Controller".length());
        }
        return name;
    }

    private String getDescription(HandlerMethod handlerMethod) {
        Method method = handlerMethod.getMethod();

        String operationSummary = readMethodAnnotationProperty(
                method, "io.swagger.v3.oas.annotations.Operation", "summary", String.class);
        if (operationSummary != null) return operationSummary;

        String apiOperationValue = readMethodAnnotationProperty(
                method, "io.swagger.annotations.ApiOperation", "value", String.class);
        if (apiOperationValue != null) return apiOperationValue;

        return method.getName();
    }

    @SuppressWarnings("unchecked")
    private String readSwagger2ApiTag(Class<?> targetClass) {
        try {
            Class<? extends Annotation> annotationClass =
                    (Class<? extends Annotation>) Class.forName("io.swagger.annotations.Api");
            Annotation annotation = targetClass.getAnnotation(annotationClass);
            if (annotation != null) {
                Method tagsMethod = annotationClass.getMethod("tags");
                String[] tags = (String[]) tagsMethod.invoke(annotation);
                if (tags != null && tags.length > 0 && !tags[0].isEmpty()) {
                    return tags[0];
                }
            }
        } catch (ClassNotFoundException ignored) {
        } catch (Exception e) {
            log.debug("[PermClient] 读取@Api注解失败: {}", e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T> T readAnnotationProperty(Class<?> targetClass, String annotationClassName, String propertyName, Class<T> returnType) {
        try {
            Class<? extends Annotation> annotationClass =
                    (Class<? extends Annotation>) Class.forName(annotationClassName);
            Annotation annotation = targetClass.getAnnotation(annotationClass);
            if (annotation != null) {
                Method propMethod = annotationClass.getMethod(propertyName);
                Object value = propMethod.invoke(annotation);
                if (value != null && !value.toString().isEmpty()) {
                    return returnType.cast(value);
                }
            }
        } catch (ClassNotFoundException ignored) {
        } catch (Exception e) {
            log.debug("[PermClient] 读取注解 {}#{} 失败: {}", annotationClassName, propertyName, e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T> T readMethodAnnotationProperty(Method targetMethod, String annotationClassName, String propertyName, Class<T> returnType) {
        try {
            Class<? extends Annotation> annotationClass =
                    (Class<? extends Annotation>) Class.forName(annotationClassName);
            Annotation annotation = targetMethod.getAnnotation(annotationClass);
            if (annotation != null) {
                Method propMethod = annotationClass.getMethod(propertyName);
                Object value = propMethod.invoke(annotation);
                if (value != null && !value.toString().isEmpty()) {
                    return returnType.cast(value);
                }
            }
        } catch (ClassNotFoundException ignored) {
        } catch (Exception e) {
            log.debug("[PermClient] 读取注解 {}#{} 失败: {}", annotationClassName, propertyName, e.getMessage());
        }
        return null;
    }
}
