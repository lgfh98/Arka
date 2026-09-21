import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'inventory',
    pathMatch: 'full'
  },
  {
    path: 'inventory',
    loadComponent: () =>
      import('./features/inventory/product-catalog.component').then(m => m.ProductCatalogComponent)
  },
  {
    path: 'orders',
    loadComponent: () =>
      import('./features/ordering/order-dashboard.component').then(m => m.OrderDashboardComponent)
  },
  {
    path: 'cart',
    loadComponent: () =>
      import('./features/cart/cart-view.component').then(m => m.CartViewComponent)
  },
  {
    path: 'analytics',
    loadComponent: () =>
      import('./features/analytics/analytics-dashboard.component').then(m => m.AnalyticsDashboardComponent)
  },
  {
    path: 'notifications',
    loadComponent: () =>
      import('./features/notification/notification-center.component').then(m => m.NotificationCenterComponent)
  },
  {
    path: '**',
    redirectTo: 'inventory'
  }
];
