export default {
  path: '/appVersionRelease',
  auth: false,
  preload: false,
  isStatic: true,
  key: 'appVersionRelease',
  layout: 'inner',
  component: () => import('./VersionRelease'),
}
