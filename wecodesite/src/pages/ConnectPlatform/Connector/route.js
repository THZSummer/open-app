/**
 * 连接流管理模块路由配置
 */
export default {
  path: '/connect/connectors',
  auth: false,
  preload: false,
  isStatic: true,
  key: 'connect-connectors',
  component: () => import('./index'),
};
