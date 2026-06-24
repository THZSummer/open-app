/**
 * 连接流编辑器模块路由配置
 */
export default {
  path: '/connect/history/flows/editor',
  auth: false,
  preload: false,
  isStatic: true,
  key: 'connect-flow-editor',
  component: () => import('./index'),
};
