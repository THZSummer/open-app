/**
 * 连接流管理模块路由配置
 */
export default {
  path: '/connectorEditor',
  auth: false,
  preload: false,
  isStatic: true,
  layout: 'inner',
  key: 'connect-connector-editor',
  component: () => import('./index'),
};
