/**
 * 应用列表 - 路由配置
 */
export default {
  path: '/appList',
  auth: false,
  preload: false,
  isStatic: true,
  key: 'appList',
  layout: 'outer', // outer 一级菜单 inner 二级菜单
  component: () => import('./AppList')
};

