import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ModeSelectorComponent } from '../../features/mode-selector/mode-selector.component';

@Component({
  selector: 'app-toolbar',
  standalone: true,
  imports: [RouterLink, MatToolbarModule, MatIconModule, MatButtonModule, MatTooltipModule, ModeSelectorComponent],
  template: `
    <mat-toolbar class="toolbar">
      <a routerLink="/" class="brand">
        <mat-icon class="logo-icon">headphones</mat-icon>
        <span class="title">Ayan DJ Tools</span>
      </a>
      <span class="spacer"></span>
      <app-mode-selector />
      <button mat-icon-button routerLink="/history" matTooltip="Historique">
        <mat-icon>history</mat-icon>
      </button>
      <button mat-icon-button routerLink="/settings" matTooltip="Parametres">
        <mat-icon>settings</mat-icon>
      </button>
    </mat-toolbar>
  `,
  styles: `
    .toolbar {
      background: var(--mat-sys-surface-container);
      border-bottom: 1px solid var(--mat-sys-outline-variant);
      gap: 8px;
    }
    .brand {
      display: flex;
      align-items: center;
      gap: 8px;
      text-decoration: none;
      color: inherit;
    }
    .logo-icon {
      color: var(--mat-sys-primary);
    }
    .title {
      font-weight: 500;
      font-size: 1.1rem;
    }
    .spacer {
      flex: 1;
    }
  `,
})
/** Barre d'outils principale — logo, selecteur de mode, liens vers historique et parametres. */
export class ToolbarComponent {}
