/**
 * 连接流管理模块路由配置
 */
export default {
  path: '/connect/flows',
  auth: false,
  preload: false,
  isStatic: true,
  key: 'connect-flows',
  component: () => import('./index'),
};
