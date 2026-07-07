export default {
  path: '/appBasicInfo',
  auth: false,
  preload: false,
  isStatic: true,
  key: 'appBasicInfo',
  layout: 'inner', // outer 一级菜单 inner 二级菜单
  component: () => import('./BasicInfo')
};
