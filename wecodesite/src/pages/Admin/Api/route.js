export default {
    path: '/admin/apis',
    auth: false,
    preload: false,
    isStatic: true,
    key: 'admin-apis',
    component: () => import('./index'),
}
