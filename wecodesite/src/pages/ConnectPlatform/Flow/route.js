/**
 * 连接流管理模块路由配置
 */
export default {
  path: '/flowList',
  auth: false,
  preload: false,
  isStatic: true,
  layout: 'inner',
  key: 'connect-flows',
  component: () => import('./index'),
};
