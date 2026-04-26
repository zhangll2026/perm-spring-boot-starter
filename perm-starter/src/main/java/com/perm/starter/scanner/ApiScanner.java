package com.perm.starter.scanner;

import com.perm.common.entity.PermResource;
import com.perm.starter.repository.PermResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * API接口扫描器 - 启动时自动扫描所有Controller的接口
 * <p>
 * 通过反射读取Swagger注解，不强制依赖Swagger
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiScanner {

    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    private final PermResourceRepository permResourceRepository;

    /**
     * 扫描所有Controller接口并持久化
     * 先加载所有现有资源到内存Map中对比，避免循环查库
     * 清理不再存在的旧接口记录
     */
    @Transactional
    public void scanAndPersist() {
        log.info("[PermStarter] 开始扫描API接口...");

        Map<RequestMappingInfo, HandlerMethod> handlerMethods = requestMappingHandlerMapping.getHandlerMethods();

        // 一次性加载所有现有资源到Map，避免循环查库
        List<PermResource> existingResources = permResourceRepository.findAllOrderByGroupAndPath();
        Map<String, PermResource> existingMap = existingResources.stream()
                .collect(Collectors.toMap(
                        r -> r.getMethod().toUpperCase() + ":" + r.getPath(),
                        r -> r,
                        (a, b) -> a
                ));

        // 收集当前扫描到的所有key
        Set<String> currentKeys = new HashSet<>();
        List<PermResource> newResources = new ArrayList<>();
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
                currentKeys.add(key);

                PermResource existing = existingMap.get(key);
                if (existing != null) {
                    // 更新描述等信息
                    existing.setDescription(description);
                    existing.setControllerClass(controllerClass);
                    existing.setMethodName(methodName);
                    existing.setGroupName(groupName);
                    // 不需要save，JPA脏检查会自动更新
                } else {
                    PermResource resource = PermResource.builder()
                            .path(path)
                            .method(httpMethod)
                            .description(description)
                            .controllerClass(controllerClass)
                            .methodName(methodName)
                            .groupName(groupName)
                            .enabled(true)
                            .build();
                    newResources.add(resource);
                }
                count++;
            }
        }

        // 清理不再存在的旧接口
        Set<String> existingKeys = existingMap.keySet();
        Set<String> removedKeys = new HashSet<>(existingKeys);
        removedKeys.removeAll(currentKeys);
        if (!removedKeys.isEmpty()) {
            for (String key : removedKeys) {
                PermResource removed = existingMap.get(key);
                permResourceRepository.deleteById(removed.getId());
                log.debug("[PermStarter] 清理无效接口: {} {}", removed.getMethod(), removed.getPath());
            }
            log.info("[PermStarter] 清理了 {} 个无效接口", removedKeys.size());
        }

        // 批量保存新资源
        if (!newResources.isEmpty()) {
            permResourceRepository.saveAll(newResources);
        }

        log.info("[PermStarter] API接口扫描完成，共扫描到 {} 个接口，新增 {} 个", count, newResources.size());
    }

    private boolean isUserController(HandlerMethod handlerMethod) {
        Class<?> beanType = handlerMethod.getBeanType();
        Package pkg = beanType.getPackage();
        if (pkg == null) return true;
        String packageName = pkg.getName();
        if (packageName.startsWith("org.springframework.")) return false;
        if (packageName.startsWith("com.perm.starter.")) return false;
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
            log.debug("[PermStarter] 读取@Api注解失败: {}", e.getMessage());
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
            log.debug("[PermStarter] 读取注解 {}#{} 失败: {}", annotationClassName, propertyName, e.getMessage());
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
            log.debug("[PermStarter] 读取注解 {}#{} 失败: {}", annotationClassName, propertyName, e.getMessage());
        }
        return null;
    }
}
