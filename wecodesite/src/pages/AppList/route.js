/**
 * 应用列表 - 路由配置
 */
export default {
  path: '/',
  auth: true,
  preload: false,
  layout: 'inner',
  component: () => import('./AppList'),
};

