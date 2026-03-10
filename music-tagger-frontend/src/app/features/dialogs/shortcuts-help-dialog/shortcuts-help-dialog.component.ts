import { Component } from '@angular/core';
import { MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';

interface Shortcut {
  keys: string;
  description: string;
}

@Component({
  selector: 'app-shortcuts-help-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>Raccourcis clavier</h2>
    <mat-dialog-content>
      <table class="shortcuts-table">
        <tbody>
          @for (s of shortcuts; track s.keys) {
            <tr>
              <td><kbd>{{ s.keys }}</kbd></td>
              <td>{{ s.description }}</td>
            </tr>
          }
        </tbody>
      </table>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Fermer</button>
    </mat-dialog-actions>
  `,
  styles: `
    .shortcuts-table {
      border-collapse: collapse;
      width: 100%;
      min-width: 320px;
    }
    td {
      padding: 6px 12px;
      vertical-align: middle;
    }
    kbd {
      display: inline-block;
      padding: 2px 8px;
      font-family: monospace;
      font-size: 0.85rem;
      background: var(--mat-sys-surface-container-high);
      border: 1px solid var(--mat-sys-outline-variant);
      border-radius: 4px;
      white-space: nowrap;
    }
  `,
})
export class ShortcutsHelpDialogComponent {
  protected shortcuts: Shortcut[] = [
    { keys: 'Ctrl + P', description: 'Mode PLAN' },
    { keys: 'Ctrl + M', description: 'Mode MANUEL' },
    { keys: 'Ctrl + A', description: 'Mode AUTO (APPLY)' },
    { keys: 'Ctrl + H', description: 'Ouvrir l\'historique' },
    { keys: 'Ctrl + S', description: 'Ouvrir les statistiques' },
    { keys: 'Ctrl + ,', description: 'Ouvrir les paramètres' },
    { keys: 'Ctrl + O', description: 'Sélectionner des fichiers' },
    { keys: '?', description: 'Afficher cette aide' },
  ];
}
