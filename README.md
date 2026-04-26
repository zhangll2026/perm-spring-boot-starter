# perm-spring-boot-starter

> 零侵入的 Spring Boot 权限管理组件 —— 引入即用，自动扫描，界面管理

## 是什么

一个 Spring Boot Starter 形式的权限管理 jar 包，业务项目引入后：

1. **自动扫描** 所有 Controller 接口（方法级别）
2. **自动建表** 管理用户、角色、接口资源
3. **Vue 管理界面** 可视化配置「哪个角色能访问哪些接口」
4. **拦截器鉴权** 请求进入时自动校验权限，业务代码零改动

同时支持 **单体架构** 和 **微服务架构** 两种部署模式。

---

## 模块结构

```
perm-parent
├── perm-common           # 共享核心（实体、DTO、异常、工具类）
├── perm-starter          # 单体模式 Starter（一个 jar 搞定）
├── perm-server           # 微服务版权限中心（独立部署，Redis + MySQL）
└── perm-client-starter   # 微服务客户端（轻量拦截 + 接口注册 + 上下文传播）
```

| 模块 | 适用场景 | 部署方式 | 数据存储 |
|------|---------|---------|---------|
| `perm-starter` | 单体应用 | 引入 jar 即可 | 业务项目自带数据库 |
| `perm-server` | 微服务架构 | 独立部署服务 | MySQL + Redis |
| `perm-client-starter` | 微服务各业务服务 | 引入 jar | 无本地存储，远程调用 perm-server |

---

## 快速开始

### 单体模式（推荐先用这个）

**1. 引入依赖**

```xml
<dependency>
    <groupId>com.perm</groupId>
    <artifactId>perm-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

**2. 配置 application.yml**

```yaml
perm:
  jwt-secret: your-secret-key-at-least-32-characters-long!!
  # admin-password: admin123    # 可选，不配则自动生成随机密码
  # exclude-paths:              # 可选，放行路径（支持 AntPath）
  #   - /public/**
  #   - /health
  # allowed-origins:             # 可选，CORS 跨域
  #   - http://localhost:5173

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/your_db?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
```

**3. 启动项目**

启动后控制台会输出：

```
==============================================
[PermStarter] 权限管理组件初始化完成
[PermStarter] 管理界面: /perm-admin/index.html
[PermStarter] 默认账号: admin
[PermStarter] 随机密码: xxxxxxxx （请立即登录修改！）
==============================================
```

**4. 打开管理界面**

浏览器访问 `http://localhost:port/perm-admin/index.html`，登录后即可管理。

---

### 微服务模式

微服务模式下，需要先部署 `perm-server`，然后各业务服务引入 `perm-client-starter`。

#### 部署 perm-server（权限中心）

**1. 配置 application.yml**

```yaml
server:
  port: 9090

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/perm_db?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
  data:
    redis:
      host: localhost
      port: 6379

perm:
  jwt-secret: perm-server-secret-key-must-be-at-least-32-characters!!
  admin-password: admin123
```

**2. 启动 perm-server**

```bash
java -jar perm-server-1.0.0.jar
```

管理界面：`http://localhost:9090/perm-admin/index.html`

#### 业务服务引入 perm-client-starter

**1. 添加依赖**

```xml
<dependency>
    <groupId>com.perm</groupId>
    <artifactId>perm-client-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

**2. 配置 application.yml**

```yaml
spring:
  application:
    name: order-service    # 自动作为 serviceId

perm:
  jwt-secret: perm-server-secret-key-must-be-at-least-32-characters!!  # 与 perm-server 一致
  server-url: http://localhost:9090    # perm-server 地址
  service-id: order-service            # 也可手动指定，默认取 spring.application.name
  trust-gateway: true                  # 信任网关传递的 X-User-Id / X-Username 头
  cache-expire-seconds: 300            # 权限缓存过期时间
  exclude-paths:                       # 放行路径
    - /public/**
```

**3. 启动业务服务**

启动时会自动：
- 扫描本服务的所有 Controller 接口
- 远程注册到 perm-server
- 在 perm-server 管理界面可看到按服务分组的接口列表

---

## 配置项参考

### perm-starter（单体模式）

| 配置项 | 默认值 | 说明 |
|-------|--------|------|
| `perm.enabled` | `true` | 是否启用权限管理 |
| `perm.jwt-secret` | 无（必填） | JWT 密钥，至少 32 字符 |
| `perm.jwt-expiration` | `86400000` | Token 过期时间（毫秒），默认 24 小时 |
| `perm.auto-scan` | `true` | 启动时是否自动扫描接口 |
| `perm.init-admin` | `true` | 是否初始化默认管理员 |
| `perm.admin-password` | 随机生成 | 管理员初始密码 |
| `perm.allow-anonymous` | `false` | 未登录时是否放行 |
| `perm.exclude-paths` | `[]` | 放行路径列表（AntPath 格式） |
| `perm.api-path` | `/perm-api` | 管理 API 路径前缀 |
| `perm.admin-path` | `/perm-admin` | 管理页面路径前缀 |
| `perm.allowed-origins` | `[]` | CORS 允许的来源域名 |

### perm-client-starter（微服务客户端）

| 配置项 | 默认值 | 说明 |
|-------|--------|------|
| `perm.enabled` | `true` | 是否启用 |
| `perm.jwt-secret` | 无（必填） | JWT 密钥，与 perm-server 一致 |
| `perm.server-url` | `http://localhost:9090` | perm-server 地址 |
| `perm.service-id` | `spring.application.name` | 当前服务 ID |
| `perm.trust-gateway` | `true` | 是否信任网关传递的用户头 |
| `perm.user-id-header` | `X-User-Id` | 网关传递用户 ID 的请求头 |
| `perm.username-header` | `X-Username` | 网关传递用户名的请求头 |
| `perm.internal-header` | `X-Internal-Call` | 内部调用标识头 |
| `perm.cache-expire-seconds` | `300` | 权限缓存过期时间（秒） |
| `perm.exclude-paths` | `[]` | 放行路径列表 |

---

## API 接口

### 认证相关

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/perm-api/auth/login` | 登录 |
| POST | `/perm-api/auth/logout` | 登出 |
| GET | `/perm-api/auth/info` | 获取当前用户信息 |
| PUT | `/perm-api/auth/password` | 修改密码 |

### 用户管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/perm-api/users` | 用户列表 |
| POST | `/perm-api/users` | 创建用户 |
| PUT | `/perm-api/users/{id}` | 更新用户 |
| DELETE | `/perm-api/users/{id}` | 删除用户 |
| POST | `/perm-api/users/{id}/roles` | 分配角色 |
| PUT | `/perm-api/users/{id}/reset-password` | 重置密码 |

### 角色管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/perm-api/roles` | 角色列表 |
| POST | `/perm-api/roles` | 创建角色 |
| PUT | `/perm-api/roles/{id}` | 更新角色 |
| DELETE | `/perm-api/roles/{id}` | 删除角色 |
| POST | `/perm-api/roles/{id}/resources` | 分配接口权限 |

### 资源管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/perm-api/resources` | 按服务+分组查看接口 |
| GET | `/perm-api/resources/list` | 全部接口列表 |
| GET | `/perm-api/resources/services` | 服务 ID 列表（微服务模式） |
| PUT | `/perm-api/resources/{id}/toggle` | 启用/禁用接口 |
| POST | `/perm-api/resources/register` | 客户端注册接口（内部调用） |

> 单体模式下资源由启动时自动扫描生成，微服务模式下由各 client 启动时远程注册。

---

## 微服务架构说明

```
                    ┌─────────────┐
                    │   Gateway   │  验证JWT，注入 X-User-Id / X-Username
                    └──────┬──────┘
                           │
            ┌──────────────┼──────────────┐
            ▼              ▼              ▼
    ┌──────────────┐ ┌──────────┐ ┌──────────────┐
    │ order-service │ │ pay-svc  │ │ user-service │
    │  (client)    │ │ (client) │ │  (client)    │
    └──────┬───────┘ └────┬─────┘ └──────┬───────┘
           │              │              │
           │   启动时注册接口    │              │
           └──────────────┼──────────────┘
                          ▼
                  ┌──────────────┐
                  │  perm-server │  权限中心（MySQL + Redis）
                  │  :9090       │  统一管理用户/角色/权限
                  └──────────────┘
```

### 调用链路

1. **请求进入** → Gateway 验证 JWT，将 `X-User-Id` / `X-Username` 注入请求头
2. **业务服务** → ClientPermissionInterceptor 读取网关头，设置 UserContext
3. **权限校验** → 从 perm-server 远程查询用户权限（本地缓存 300s），校验当前请求
4. **Feign 调用** → PermFeignInterceptor 自动传播 `X-User-Id` / `X-Username` / `X-Internal-Call`
5. **内部调用** → InternalCallInterceptor 检测 `X-Internal-Call` 头，恢复 UserContext，跳过鉴权

---

## 核心特性

### 安全

- **JWT 鉴权** — HMAC-SHA 签名，jti 支持 Token 黑名单（单体内存 / 微服务 Redis）
- **BCrypt 加密** — 密码存储使用 BCrypt，不存明文
- **防暴力破解** — 连续 5 次登录失败锁定 30 分钟
- **管理 API 鉴权** — `/perm-api/**` 独立拦截器，必须 Token 才能访问
- **密钥校验** — `jwt-secret` 未配置或不足 32 字符时启动失败
- **JSON 安全输出** — 所有错误响应经过转义，防 XSS

### 性能

- **批量加载** — 接口扫描一次性加载现有资源到内存对比，避免循环查库
- **权限缓存** — AtomicLong 版本号 + 懒刷新，角色变更时递增版本号触发刷新
- **远程缓存** — 微服务客户端本地缓存权限 300s，减少对 perm-server 的调用

### 零侵入

- **自动扫描** — 启动时扫描所有 `@RequestMapping`，支持 Swagger 注解读取（反射方式，不强制依赖）
- **自动建表** — JPA ddl-auto 自动创建权限相关表
- **自动注册** — 微服务客户端启动时将接口远程注册到 perm-server
- **上下文传播** — Feign 调用自动传播用户标识，业务代码无需处理

---

## 数据库表

组件自动创建以下表（使用业务项目自带数据库）：

| 表名 | 说明 |
|------|------|
| `perm_resource` | API 接口资源（自动扫描/注册生成） |
| `perm_role` | 角色 |
| `perm_user` | 用户 |
| `perm_user_role` | 用户-角色关联 |
| `perm_role_resource` | 角色-接口关联 |

---

## 技术栈

| 层面 | 技术 |
|------|------|
| 后端 | Spring Boot 3.2.5 / Java 17 |
| 持久层 | Spring Data JPA / Hibernate |
| 认证 | JWT (jjwt 0.12.5) + BCrypt |
| 缓存 | ConcurrentHashMap（单体）/ Redis（微服务） |
| 前端 | Vue 3 + Element Plus + Vite |
| 构建 | Maven 多模块 |

---

## 本地构建

```bash
git clone https://github.com/zhangll2026/perm-spring-boot-starter.git
cd perm-spring-boot-starter
mvn clean package -DskipTests
```

构建产物：

```
perm-common-1.0.0.jar          # 共享核心
perm-starter-1.0.0.jar         # 单体模式 Starter
perm-server-1.0.0.jar          # 微服务权限中心（可执行 jar）
perm-client-starter-1.0.0.jar  # 微服务客户端 Starter
```

---

## License

MIT
