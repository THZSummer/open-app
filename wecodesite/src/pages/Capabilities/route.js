export default {
  path: '/abilities',
  auth: false,
  preload: false,
  isStatic: true,
  key: 'abilities',
  layout: 'inner', // outer 一级菜单 inner 二级菜单
  component: () => import('./BasicInfo')
};
