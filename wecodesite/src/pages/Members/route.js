export default {
  path: '/membersManagement',
  auth: false,
  preload: false,
  isStatic: true,
  key: 'membersManagement',
  layout: 'inner',
  component: () => import('./Members'),
}
