export default {
    path: '/admin/approvals',
    auth: false,
    preload: false,
    isStatic: true,
    key: 'admin-approvals',
    layout: 'inner',
    component: () => import('./index'),
}
