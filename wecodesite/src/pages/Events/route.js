export default {
    path: '/events',
    auth: false,
    preload: false,
    isStatic: true,
    key: 'events',
    layout: 'inner',
    component: () => import('./index'),
}
