import { Component, inject, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { FileSelectionService } from '../../services/file-selection.service';

const AUDIO_EXTENSIONS = new Set(['.mp3', '.flac', '.wav', '.aiff', '.m4a', '.ogg']);

function isAudioFile(name: string): boolean {
  const ext = name.slice(name.lastIndexOf('.')).toLowerCase();
  return AUDIO_EXTENSIONS.has(ext);
}

@Component({
  selector: 'app-drag-drop-zone',
  standalone: true,
  imports: [MatIconModule],
  template: `
    <div
      class="drop-zone"
      [class.dragging]="dragging()"
      (dragover)="onDragOver($event)"
      (dragleave)="onDragLeave()"
      (drop)="onDrop($event)">
      <mat-icon>upload_file</mat-icon>
      <span>Glisser des fichiers audio ici</span>
    </div>
  `,
  styles: `
    .drop-zone {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 4px;
      padding: 12px;
      border: 2px dashed var(--mat-sys-outline-variant);
      border-radius: 8px;
      cursor: pointer;
      transition: border-color 150ms, background 150ms;
      color: var(--mat-sys-on-surface-variant);
      font-size: 0.75rem;
      mat-icon {
        font-size: 20px;
        width: 20px;
        height: 20px;
      }
    }
    .drop-zone.dragging {
      border-color: var(--mat-sys-primary);
      background: color-mix(in srgb, var(--mat-sys-primary) 10%, transparent);
      color: var(--mat-sys-primary);
    }
  `,
})
export class DragDropZoneComponent {
  private fileService = inject(FileSelectionService);
  protected dragging = signal(false);

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(true);
  }

  onDragLeave(): void {
    this.dragging.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(false);
    if (!event.dataTransfer) return;

    const paths: string[] = [];
    for (const file of Array.from(event.dataTransfer.files)) {
      const path = (file as unknown as { path?: string }).path ?? file.name;
      if (isAudioFile(file.name)) {
        paths.push(path);
      }
    }
    if (paths.length > 0) {
      this.fileService.addFiles(paths);
    }
  }
}
