# Frontend Angular — Electron Integration

## Main Process

```typescript
// electron/main.ts
import { app, BrowserWindow, ipcMain, dialog } from 'electron';
import * as path from 'path';

let mainWindow: BrowserWindow;

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1400, height: 900,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      preload: path.join(__dirname, 'preload.js')
    }
  });

  ipcMain.handle('select-audio-files', async () => {
    const result = await dialog.showOpenDialog(mainWindow, {
      properties: ['openFile', 'multiSelections'],
      filters: [
        { name: 'Audio Files', extensions: ['mp3', 'flac', 'wav', 'aiff', 'm4a', 'ogg'] }
      ]
    });
    return result.filePaths;
  });

  if (process.env['NODE_ENV'] === 'development') {
    mainWindow.loadURL('http://localhost:4200');
  } else {
    mainWindow.loadFile(path.join(__dirname, '../dist/index.html'));
  }
}

app.whenReady().then(createWindow);
app.on('window-all-closed', () => { if (process.platform !== 'darwin') app.quit(); });
```

## Preload Script

```typescript
// electron/preload.ts
import { contextBridge, ipcRenderer } from 'electron';

contextBridge.exposeInMainWorld('electron', {
  selectAudioFiles: () => ipcRenderer.invoke('select-audio-files'),
  openFile: (path: string) => ipcRenderer.invoke('open-file', path)
});

declare global {
  interface Window {
    electron: {
      selectAudioFiles: () => Promise<string[]>;
      openFile: (path: string) => Promise<void>;
    };
  }
}
```

## Service Angular - File Selection

```typescript
@Injectable({ providedIn: 'root' })
export class FileSelectionService {
  selectedFiles = signal<string[]>([]);

  async selectFiles(): Promise<string[]> {
    const files = await window.electron.selectAudioFiles();
    this.selectedFiles.set(files);
    return files;
  }

  clearSelection(): void { this.selectedFiles.set([]); }
}
```

## File Picker Component

```typescript
@Component({
  selector: 'app-file-picker',
  standalone: true,
  imports: [MatButtonModule, MatListModule, MatIconModule],
  template: `
    <div class="file-picker">
      <button mat-raised-button color="primary" (click)="selectFiles()">
        <mat-icon>folder_open</mat-icon> Selectionner fichiers audio
      </button>
      @if (selectedFiles().length > 0) {
        <h3>{{ selectedFiles().length }} fichiers selectionnes</h3>
        <mat-list>
          @for (file of selectedFiles(); track file) {
            <mat-list-item>
              <mat-icon matListItemIcon>audiotrack</mat-icon>
              <span matListItemTitle>{{ extractFilename(file) }}</span>
            </mat-list-item>
          }
        </mat-list>
        <button mat-raised-button color="primary" (click)="scanFiles()" [disabled]="isScanning()">
          Scanner les fichiers
        </button>
      }
    </div>
  `
})
export class FilePickerComponent {
  private fileSelectionService = inject(FileSelectionService);
  private musicFileService = inject(MusicFileService);

  selectedFiles = this.fileSelectionService.selectedFiles;
  isScanning = signal(false);
  filesScanned = output<MusicFileInfo[]>();

  async selectFiles() { await this.fileSelectionService.selectFiles(); }

  scanFiles() {
    this.isScanning.set(true);
    this.musicFileService.scanFiles(this.selectedFiles()).subscribe({
      next: (result) => { this.filesScanned.emit(result.files); this.isScanning.set(false); },
      error: () => this.isScanning.set(false)
    });
  }

  extractFilename(path: string): string {
    return path.split('/').pop() || path.split('\\').pop() || path;
  }
}
```
