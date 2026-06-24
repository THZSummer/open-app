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

export const ROUTE_PATH = '/';

export const ROUTE_CONFIG = {
  path: ROUTE_PATH,
  name: '应用列表',
};
