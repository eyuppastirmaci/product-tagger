import { Routes } from '@angular/router';
import { Shell } from './core/layout/shell';
import { authGuard } from './core/state/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    data: { mode: 'login' },
    loadComponent: () => import('./features/auth/auth-page').then((m) => m.AuthPage),
  },
  {
    path: 'register',
    data: { mode: 'register' },
    loadComponent: () => import('./features/auth/auth-page').then((m) => m.AuthPage),
  },
  {
    path: '',
    component: Shell,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'products' },
      {
        path: 'upload',
        data: { title: 'shell.nav.upload' },
        loadComponent: () => import('./features/upload/upload-page').then((m) => m.UploadPage),
      },
      {
        path: 'products',
        data: { title: 'shell.nav.products' },
        loadComponent: () => import('./features/products/products-page').then((m) => m.ProductsPage),
      },
      {
        path: 'review',
        data: { title: 'shell.nav.reviewQueue' },
        loadComponent: () => import('./features/review/review-queue-page').then((m) => m.ReviewQueuePage),
      },
      {
        path: 'review/:id',
        data: { title: 'review.breadcrumb' },
        loadComponent: () => import('./features/review/review-page').then((m) => m.ReviewPage),
      },
    ],
  },
];
