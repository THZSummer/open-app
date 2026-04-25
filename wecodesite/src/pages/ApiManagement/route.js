export default {
    path: '/api-management',
    auth: false,
    preload: false,
    isStatic: true,
    key: 'api-management',
    layout: 'inner',
    component: () => import('./index'),
}
