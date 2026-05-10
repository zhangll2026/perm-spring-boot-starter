/**
 * perm-spring-boot-starter 前端 SDK 核心类
 */
class Perm {
  constructor(options = {}) {
    this.options = {
      apiBaseUrl: options.apiBaseUrl || '',
      tokenKey: options.tokenKey || 'perm_token',
      onTokenExpired: options.onTokenExpired || (() => {}),
      ...options
    };

    this._permissions = new Set();
    this._roles = new Set();
    this._token = null;
  }

  /**
   * 初始化 SDK，获取当前用户的权限
   */
  async init() {
    const token = this.getToken();
    if (token) {
      this._token = token;
      await this.fetchPermissions();
    }
  }

  /**
   * 获取 Token
   */
  getToken() {
    return localStorage.getItem(this.options.tokenKey);
  }

  /**
   * 设置 Token
   */
  setToken(token) {
    this._token = token;
    localStorage.setItem(this.options.tokenKey, token);
  }

  /**
   * 清除 Token
   */
  clearToken() {
    this._token = null;
    localStorage.removeItem(this.options.tokenKey);
    this._permissions.clear();
    this._roles.clear();
  }

  /**
   * 从后端获取用户权限列表
   */
  async fetchPermissions() {
    try {
      const response = await fetch(`${this.options.apiBaseUrl}/perm-api/auth/permissions`, {
        headers: {
          Authorization: `Bearer ${this._token}`
        }
      });

      if (response.status === 401) {
        this.options.onTokenExpired();
        return;
      }

      const data = await response.json();
      if (data.code === 200) {
        this._permissions = new Set(data.data.permissions || []);
        this._roles = new Set(data.data.roles || []);
      }
    } catch (e) {
      console.error('[Perm SDK] 获取权限失败:', e);
    }
  }

  /**
   * 检查是否有指定权限
   * @param {string} permission 权限标识，如：GET:/api/products
   * @returns {boolean}
   */
  hasPermission(permission) {
    return this._permissions.has(permission);
  }

  /**
   * 检查是否有任意一个权限
   * @param {string[]} permissions
   * @returns {boolean}
   */
  hasAnyPermission(permissions) {
    return permissions.some(p => this.hasPermission(p));
  }

  /**
   * 检查是否有指定角色
   * @param {string} role
   * @returns {boolean}
   */
  hasRole(role) {
    return this._roles.has(role);
  }

  /**
   * 检查是否有任意一个角色
   * @param {string[]} roles
   * @returns {boolean}
   */
  hasAnyRole(roles) {
    return roles.some(r => this.hasRole(r));
  }

  /**
   * 刷新权限列表
   */
  async refresh() {
    await this.fetchPermissions();
  }

  /**
   * 获取所有权限
   * @returns {string[]}
   */
  getPermissions() {
    return Array.from(this._permissions);
  }

  /**
   * 获取所有角色
   * @returns {string[]}
   */
  getRoles() {
    return Array.from(this._roles);
  }
}

// 默认实例
let defaultInstance = null;

/**
 * 初始化默认实例
 */
function initPerm(options) {
  defaultInstance = new Perm(options);
  return defaultInstance;
}

/**
 * 获取默认实例
 */
function getPerm() {
  if (!defaultInstance) {
    throw new Error('Perm SDK 未初始化，请先调用 initPerm()');
  }
  return defaultInstance;
}

module.exports = {
  Perm,
  initPerm,
  getPerm
};
