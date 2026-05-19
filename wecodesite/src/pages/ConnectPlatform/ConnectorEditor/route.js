/**
 * 连接流管理模块路由配置
 */
export default {
  path: '/connect/connector-editor',
  auth: false,
  preload: false,
  isStatic: true,
  key: 'connect-connector-editor',
  component: () => import('./index'),
};
