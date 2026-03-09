import { Routes } from '@angular/router';
import { ChatComponent } from './features/chat/chat.component';

export const routes: Routes = [
  { path: '', component: ChatComponent },
  { path: 'plan/:id', loadComponent: () => import('./features/plan-review/plan-review.component') },
  { path: 'history', loadComponent: () => import('./features/history/history.component') },
  { path: 'settings', loadComponent: () => import('./features/settings/settings.component') },
];
