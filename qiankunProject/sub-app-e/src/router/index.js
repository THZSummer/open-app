import { createRouter, createWebHistory, createMemoryHistory } from 'vue-router';
import { qiankunWindow } from 'vite-plugin-qiankun/dist/helper';
import List from '../pages/List.vue';
import Detail from '../pages/Detail.vue';
import Edit from '../pages/Edit.vue';

// 基础路由配置表（独立运行时使用，路径不含前缀）
const baseRoutes = [
  // 列表页 - 默认首页
  { path: '/', component: List },
  // 详情页
  { path: '/detail', component: Detail },
  // 编辑页
  { path: '/edit', component: Edit }
];

/**
 * 创建路由实例
 * @param {string} basename 路由基础路径
 * @returns {Object} 路由实例
 */
function createAppRouter(basename) {
  // Vite 子应用需通过 qiankunWindow 访问 qiankun 注入的变量（非 window）
  const isQiankun = qiankunWindow.__POWERED_BY_QIANKUN__;

  if (isQiankun) {
    // 主应用使用 HashRouter，hash 已被主应用路由占用（如 #/qiankun/sub-e），
    // Vue 的 createWebHashHistory 无法像 React HashRouter 那样从 hash 中剥离 basename，
    // 改用 createMemoryHistory 不操作 URL，子应用内部导航在内存中进行
    return createRouter({
      history: createMemoryHistory(),
      routes: baseRoutes,
    });
  }

  // 独立运行时使用 createWebHistory，basename 由 Vue Router 自动剥离
  return createRouter({
    history: createWebHistory(basename),
    routes: baseRoutes,
  });
}

export default createAppRouter;
