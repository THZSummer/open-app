import { lazy } from 'react';
import { Route } from 'react-router-dom';

export default [
  {
    path: '/callbacks',
    component: lazy(() => import('./Callbacks')),
  },
];
