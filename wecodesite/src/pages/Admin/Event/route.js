export default {
    path: '/admin/events',
    auth: false,
    preload: false,
    isStatic: true,
    key: 'admin-events',
    layout: 'inner',
    component: () => import('./index'),
}
