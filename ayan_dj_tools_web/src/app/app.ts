import { NgClass } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { BackendHealthService } from './core/backend-health.service';

@Component({
  selector: 'app-root',
  imports: [
    NgClass,
    RouterLink,
    RouterLinkActive,
    RouterOutlet,
    MatSidenavModule,
    MatToolbarModule,
    MatListModule,
    MatIconModule,
  ],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  private readonly health = inject(BackendHealthService);

  protected readonly backendOk = computed(() => this.health.status() === 'up');
  protected readonly backendStatusLabel = computed(() => this.health.status().toUpperCase());

  protected readonly navItems = [
    { path: '/chat',     icon: 'chat',         label: 'Chat' },
    { path: '/history',  icon: 'history',      label: 'Historique' },
    { path: '/playlist', icon: 'queue_music',  label: 'Playlist' },
    { path: '/stats',    icon: 'bar_chart',    label: 'Stats' },
    { path: '/settings', icon: 'settings',     label: 'Paramètres' },
  ];
}
