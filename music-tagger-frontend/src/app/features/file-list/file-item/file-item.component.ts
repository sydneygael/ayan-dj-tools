import { Component, input, output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatIconButton } from '@angular/material/button';

@Component({
  selector: 'app-file-item',
  standalone: true,
  imports: [MatIconModule, MatIconButton],
  template: `
    <div class="file-item">
      <mat-icon class="file-icon">audio_file</mat-icon>
      <span class="filename" [title]="filepath()">{{ extractFilename(filepath()) }}</span>
      <button mat-icon-button class="remove-btn" (click)="removed.emit(filepath())">
        <mat-icon>close</mat-icon>
      </button>
    </div>
  `,
  styles: `
    .file-item {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 4px 0;
      border-radius: 4px;
      &:hover {
        background: var(--mat-sys-surface-container-high);
      }
    }
    .file-icon {
      color: var(--mat-sys-primary);
      font-size: 20px;
      width: 20px;
      height: 20px;
    }
    .filename {
      flex: 1;
      font-size: 0.85rem;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      font-family: 'Roboto Mono', monospace;
    }
    .remove-btn {
      opacity: 0.5;
      &:hover { opacity: 1; }
    }
  `,
})
export class FileItemComponent {
  filepath = input.required<string>();
  removed = output<string>();

  extractFilename(path: string): string {
    return path.split(/[\\/]/).pop() ?? path;
  }
}
