import { Component, input, output } from '@angular/core';
import { FileItemComponent } from './file-item/file-item.component';

@Component({
  selector: 'app-file-list',
  standalone: true,
  imports: [FileItemComponent],
  template: `
    @if (files().length === 0) {
      <p class="empty">Aucun fichier selectionne</p>
    } @else {
      <div class="file-list">
        <p class="count">{{ files().length }} fichier(s)</p>
        @for (file of files(); track file) {
          <app-file-item [filepath]="file" (removed)="fileRemoved.emit($event)" (selected)="fileSelected.emit($event)" />
        }
      </div>
    }
  `,
  styles: `
    .empty {
      color: var(--mat-sys-on-surface-variant);
      font-size: 0.85rem;
      text-align: center;
      padding: 16px 0;
    }
    .file-list {
      display: flex;
      flex-direction: column;
    }
    .count {
      font-size: 0.75rem;
      color: var(--mat-sys-on-surface-variant);
      margin: 4px 0;
    }
  `,
})
/** Liste des fichiers audio selectionnes, affichee dans la sidebar. Emet lors de la suppression ou selection d'un fichier. */
export class FileListComponent {
  files = input.required<string[]>();
  fileRemoved = output<string>();
  fileSelected = output<string>();
}
