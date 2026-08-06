let Contextroot = '';
export default {
    path: `${Contextroot}/sdk-version`,
    auth: true,
    preload: false,
    layout: 'inner',
    component: () => import('./index'),
};
