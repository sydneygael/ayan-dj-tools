import { Routes } from '@angular/router';
import { ChatComponent } from './features/chat/chat.component';

export const routes: Routes = [
  { path: '', component: ChatComponent, data: { animation: 'chat' } },
  { path: 'plan/:id', loadComponent: () => import('./features/plan-review/plan-review.component'), data: { animation: 'plan' } },
  { path: 'history', loadComponent: () => import('./features/history/history.component'), data: { animation: 'history' } },
  { path: 'settings', loadComponent: () => import('./features/settings/settings.component'), data: { animation: 'settings' } },
  { path: 'stats', loadComponent: () => import('./features/stats/stats.component'), data: { animation: 'stats' } },
];
