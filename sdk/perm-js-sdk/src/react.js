/**
 * React 支持 - usePerm Hook
 */
import { getPerm } from './index';
import { useState, useEffect } from 'react';

/**
 * usePerm Hook
 * @returns {{ hasPermission, hasAnyPermission, hasRole, hasAnyRole, permissions, roles, refresh }}
 */
export function usePerm() {
  const perm = getPerm();
  const [permissions, setPermissions] = useState(perm.getPermissions());
  const [roles, setRoles] = useState(perm.getRoles());

  useEffect(() => {
    // 可以监听权限变化事件
    const refresh = () => {
      setPermissions(perm.getPermissions());
      setRoles(perm.getRoles());
    };

    window.addEventListener('perm:refresh', refresh);
    return () => window.removeEventListener('perm:refresh', refresh);
  }, []);

  return {
    hasPermission: (p) => perm.hasPermission(p),
    hasAnyPermission: (ps) => perm.hasAnyPermission(ps),
    hasRole: (r) => perm.hasRole(r),
    hasAnyRole: (rs) => perm.hasAnyRole(rs),
    permissions,
    roles,
    refresh: async () => {
      await perm.refresh();
      setPermissions(perm.getPermissions());
      setRoles(perm.getRoles());
    }
  };
}
