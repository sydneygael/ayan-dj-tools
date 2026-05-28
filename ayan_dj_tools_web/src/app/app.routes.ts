import {Routes} from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'chat'
  },
  {
    path: 'chat',
    loadComponent: () => import('./pages/chat.page')
      .then((m) => m.ChatPageComponent)
  },
  {
    path: 'plan/:id',
    loadComponent: () => import('./pages/plan.page')
      .then((m) => m.PlanPageComponent)
  },
  {
    path: 'history',
    loadComponent: () => import('./pages/history.page')
      .then((m) => m.HistoryPageComponent)
  },
  {
    path: 'playlist',
    loadComponent: () => import('./pages/playlist.page')
      .then((m) => m.PlaylistPageComponent)
  },
  {
    path: 'stats',
    loadComponent: () => import('./pages/stats.page')
      .then((m) => m.StatsPageComponent)
  },
  {
    path: 'settings',
    loadComponent: () => import('./pages/settings.page')
      .then((m) => m.SettingsPageComponent)
  },
  {
    path: '**',
    redirectTo: 'chat'
  }
];
