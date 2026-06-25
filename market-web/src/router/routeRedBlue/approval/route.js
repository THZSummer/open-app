// let Contextroot = process.env.NODE_ENV === 'development' ? '' : '/appstore-market-admin';
let Contextroot = '';
export default {
    path: `${Contextroot}/approveManage`,
    auth: true,
    preload: false,
    layout: 'inner',
    component: () => import('./index'),
};
