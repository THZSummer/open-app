/**
 * 运行管理模块路由配置
 */
export default {
  path: '/run-management',
  auth: false,
  preload: false,
  isStatic: true,
  key: 'run-management',
  component: () => import('./index'),
};
