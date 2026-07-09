import { createRouter, createWebHistory } from 'vue-router';
import List from '../pages/List.vue';
import Detail from '../pages/Detail.vue';
import Edit from '../pages/Edit.vue';

// 路由配置表
const routes = [
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
  return createRouter({
    history: createWebHistory(basename),
    routes
  });
}

export default createAppRouter;
