/**
 * 连接流管理模块路由配置
 */
export default {
  path: '/connectorList',
  auth: false,
  preload: false,
  isStatic: true,
  layout: 'inner',
  key: 'connect-connectors',
  component: () => import('./index'),
};
