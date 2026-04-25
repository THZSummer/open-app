export default {
    path: '/callbacks',
    auth: false,
    preload: false,
    isStatic: true,
    key: 'callbacks',
    layout: 'inner',
    component: () => import('./index'),
}
