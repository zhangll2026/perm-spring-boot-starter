/**
 * Vue 3 支持 - v-perm 指令
 */
import { getPerm } from './index';

export default {
  install(app) {
    const perm = getPerm();

    /**
     * v-perm 指令
     * 用法：
     * <button v-perm="'GET:/api/products'">查看</button>
     * <button v-perm:any="['GET:/api/products', 'POST:/api/products']">查看或新增</button>
     */
    app.directive('perm', {
      mounted(el, binding) {
        checkPermission(el, binding);
      },
      updated(el, binding) {
        checkPermission(el, binding);
      }
    });

    function checkPermission(el, binding) {
      const { value, arg } = binding;
      let hasAccess = false;

      if (arg === 'any') {
        hasAccess = perm.hasAnyPermission(Array.isArray(value) ? value : [value]);
      } else {
        hasAccess = perm.hasPermission(value);
      }

      if (!hasAccess) {
        el.parentNode?.removeChild(el);
      }
    }
  }
};
