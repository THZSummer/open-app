export default {
    path: '/admin/apis',
    auth: false,
    preload: false,
    isStatic: true,
    key: 'admin-apis',
    layout: 'inner',
    component: () => import('./index'),
}
