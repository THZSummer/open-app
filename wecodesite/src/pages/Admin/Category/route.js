export default {
    path: '/admin/categories',
    auth: false,
    preload: false,
    isStatic: true,
    key: 'admin-categories',
    layout: 'inner',
    component: () => import('./index'),
}
