/**
 * ========================================
 * 连接流编辑器 V2 - 路由配置
 * ========================================
 *
 * 注册到 /connect/flow/editor 路径
 */
export default {
  path: '/flowEditor',
  auth: false,
  preload: false,
  isStatic: true,
  layout: 'inner',
  key: 'connect-flow-editor-v2',
  component: () => import('./index'),
};