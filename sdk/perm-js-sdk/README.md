# perm-js-sdk

`perm-spring-boot-starter` 的前端 SDK，支持 Vue 2/3 和 React！

---

## 📦 安装

```bash
npm install perm-js-sdk
# 或者
yarn add perm-js-sdk
```

---

## 🚀 快速开始

### 1. 初始化 SDK

```javascript
import { initPerm } from 'perm-js-sdk';

const perm = initPerm({
  apiBaseUrl: 'http://localhost:8080', // 后端地址
  tokenKey: 'perm_token', // localStorage 中 token 的 key
  onTokenExpired: () => {
    // Token 过期时的回调，比如跳转到登录页
    window.location.href = '/login';
  }
});

// 初始化后获取权限
perm.init();
```

---

### 2. Vue 3 中使用

```javascript
import { createApp } from 'vue';
import permDirective from 'perm-js-sdk/vue3';

const app = createApp(App);
app.use(permDirective);
```

```vue
<template>
  <!-- 只有拥有 GET:/api/products 权限的人才能看到 -->
  <button v-perm="'GET:/api/products'">查看商品</button>

  <!-- 拥有任意一个权限就能看到 -->
  <button v-perm:any="['GET:/api/products', 'POST:/api/products']">查看或新增</button>
</template>
```

---

### 3. React 中使用

```jsx
import { usePerm } from 'perm-js-sdk/react';

function ProductPage() {
  const { hasPermission, hasAnyPermission } = usePerm();

  return (
    <div>
      {hasPermission('GET:/api/products') && <button>查看商品</button>}
      {hasAnyPermission(['GET:/api/products', 'POST:/api/products']) && <button>查看或新增</button>}
    </div>
  );
}
```

---

### 4. 纯 JavaScript 中使用

```javascript
import { getPerm } from 'perm-js-sdk';

const perm = getPerm();

// 检查权限
if (perm.hasPermission('GET:/api/products')) {
  console.log('有权限');
}

// 刷新权限
perm.refresh();
```

---

## 📚 API 文档

### 核心方法

| 方法 | 说明 |
|------|------|
| `hasPermission(permission)` | 检查是否有指定权限 |
| `hasAnyPermission(permissions)` | 检查是否有任意一个权限 |
| `hasRole(role)` | 检查是否有指定角色 |
| `hasAnyRole(roles)` | 检查是否有任意一个角色 |
| `refresh()` | 刷新权限列表 |
| `getPermissions()` | 获取所有权限列表 |
| `getRoles()` | 获取所有角色列表 |

---

## 📖 更多示例

详细文档请参考后端项目的 README.md
