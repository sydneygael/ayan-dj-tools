import { Injectable, signal } from '@angular/core';

declare global {
  interface Window {
    electron?: {
      selectAudioFiles: () => Promise<string[]>;
    };
  }
}

/**
 * Gestion de la selection de fichiers audio.
 * En mode Electron, utilise le bridge IPC (preload.ts → contextBridge) pour ouvrir le file picker natif.
 * En mode navigateur, le file picker n'est pas disponible.
 */
@Injectable({ providedIn: 'root' })
export class FileSelectionService {
  readonly selectedFiles = signal<string[]>([]);
  readonly isElectron = signal(!!window.electron);

  /** Ouvre le file picker Electron via IPC et ajoute les fichiers selectionnes. */
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
