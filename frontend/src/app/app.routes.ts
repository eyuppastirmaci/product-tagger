import { Routes } from '@angular/router';
import { Shell } from './core/layout/shell';

export const routes: Routes = [
  {
    path: '',
    component: Shell,
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
    ],
  },
];
