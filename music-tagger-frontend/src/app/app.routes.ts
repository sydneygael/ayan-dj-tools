import { Routes } from '@angular/router';
import { ChatComponent } from './features/chat/chat.component';

export const routes: Routes = [
  { path: '', component: ChatComponent },
  { path: 'plan/:id', loadComponent: () => import('./features/chat/chat.component').then(m => m.ChatComponent) },
  { path: 'history', loadComponent: () => import('./features/chat/chat.component').then(m => m.ChatComponent) },
];
