import { Component } from '@angular/core';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { ModeSelectorComponent } from '../../features/mode-selector/mode-selector.component';

@Component({
  selector: 'app-toolbar',
  standalone: true,
  imports: [MatToolbarModule, MatIconModule, MatButtonModule, ModeSelectorComponent],
  template: `
    <mat-toolbar class="toolbar">
      <mat-icon class="logo-icon">headphones</mat-icon>
      <span class="title">Ayan DJ Tools</span>
      <span class="spacer"></span>
      <app-mode-selector />
    </mat-toolbar>
  `,
  styles: `
    .toolbar {
      background: var(--mat-sys-surface-container);
      border-bottom: 1px solid var(--mat-sys-outline-variant);
      gap: 8px;
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
export class ToolbarComponent {}
