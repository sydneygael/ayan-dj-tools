import { Injectable, signal } from '@angular/core';

declare global {
  interface Window {
    electron?: {
      selectAudioFiles: () => Promise<string[]>;
    };
  }
}

@Injectable({ providedIn: 'root' })
export class FileSelectionService {
  readonly selectedFiles = signal<string[]>([]);
  readonly isElectron = signal(!!window.electron);

  async selectFiles(): Promise<void> {
    if (window.electron) {
      const files = await window.electron.selectAudioFiles();
      if (files.length > 0) {
        this.selectedFiles.update(current => [...current, ...files]);
      }
    }
  }

  removeFile(filepath: string): void {
    this.selectedFiles.update(files => files.filter(f => f !== filepath));
  }

  clearFiles(): void {
    this.selectedFiles.set([]);
  }
}
