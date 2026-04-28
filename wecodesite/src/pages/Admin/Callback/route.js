export default {
    path: '/admin/callbacks',
    auth: false,
    preload: false,
    isStatic: true,
    key: 'admin-callbacks',
    component: () => import('./index'),
}
