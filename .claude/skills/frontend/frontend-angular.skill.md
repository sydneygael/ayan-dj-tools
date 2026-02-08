# Frontend Angular Skill

Angular 21 + Material + Electron

## Principes Angular 21

- **Standalone components** (par défaut, pas NgModules)
- **Signals** pour réactivité (signal, computed, effect)
- **inject()** au lieu de constructor injection
- **input() / output()** pour component API
- **Control flow** (@if, @for, @switch au lieu de *ngIf, *ngFor)
- **toSignal()** pour RxJS → Signals
- **viewChild() / viewChildren()** avec signals
- **Material 21** pour UI
- **WebSocket** pour temps réel

## Setup Projet

### Dependencies (package.json)
```json
{
  "dependencies": {
    "@angular/core": "^21.0.0",
    "@angular/material": "^21.0.0",
    "@angular/cdk": "^21.0.0",
    "electron": "^32.0.0",
    "rxjs": "^7.8.0"
  }
}
```

### Angular Config
```typescript
// app.config.ts
import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideHttpClient } from '@angular/common/http';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideAnimations(),
    provideHttpClient()
  ]
};
```

## Structure Composants

### Standalone Component Pattern
```typescript
// chat.component.ts
import { Component, signal, computed, effect, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatCardModule } from '@angular/material/card';
import { AgentService } from '../services/agent.service';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [
    MatButtonModule,
    MatInputModule,
    MatCardModule
  ],
  templateUrl: './chat.component.html',
  styleUrl: './chat.component.scss'
})
export class ChatComponent {
  // Injection moderne avec inject()
  private agentService = inject(AgentService);
  
  // Signals
  messages = signal<Message[]>([]);
  userInput = signal('');
  isLoading = signal(false);
  
  // Computed signals
  messageCount = computed(() => this.messages().length);
  hasMessages = computed(() => this.messages().length > 0);
  
  // Effect pour logger les changements
  constructor() {
    effect(() => {
      console.log(`Messages: ${this.messageCount()}`);
    });
  }
  
  sendMessage() {
    const content = this.userInput();
    if (!content.trim()) return;
    
    // Ajouter message utilisateur
    this.messages.update(msgs => [...msgs, {
      role: 'user',
      content,
      timestamp: new Date()
    }]);
    
    this.userInput.set('');
    this.isLoading.set(true);
    
    // Appel agent
    this.agentService.chat(content).subscribe({
      next: (response) => {
        this.messages.update(msgs => [...msgs, {
          role: 'assistant',
          content: response.message,
          timestamp: new Date()
        }]);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Chat error:', err);
        this.isLoading.set(false);
      }
    });
  }
}

interface Message {
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
}
```

### Template
```html
<!-- chat.component.html -->
<div class="chat-container">
  <div class="messages">
    @for (msg of messages(); track msg.timestamp) {
      <mat-card [class.user-message]="msg.role === 'user'"
                [class.assistant-message]="msg.role === 'assistant'">
        <mat-card-content>
          {{ msg.content }}
        </mat-card-content>
        <mat-card-footer>
          <small>{{ msg.timestamp | date:'short' }}</small>
        </mat-card-footer>
      </mat-card>
    }
    
    @if (isLoading()) {
      <mat-spinner diameter="30"></mat-spinner>
    }
  </div>
  
  <div class="input-area">
    <mat-form-field class="full-width">
      <input matInput
             [value]="userInput()"
             (input)="userInput.set($any($event.target).value)"
             (keyup.enter)="sendMessage()"
             placeholder="Message l'agent..."
             [disabled]="isLoading()">
    </mat-form-field>
    
    <button mat-raised-button
            color="primary"
            (click)="sendMessage()"
            [disabled]="!userInput() || isLoading()">
      Envoyer
    </button>
  </div>
</div>
```

## Signals Avancés (Angular 21)

### computed() - Valeurs dérivées
```typescript
import { Component, signal, computed } from '@angular/core';

@Component({...})
export class MusicLibraryComponent {
  files = signal<MusicFile[]>([]);
  
  // Computed - recalculé automatiquement
  totalFiles = computed(() => this.files().length);
  
  filesWithMissingTags = computed(() => 
    this.files().filter(f => f.missingTags.length > 0)
  );
  
  completionRate = computed(() => {
    const total = this.totalFiles();
    if (total === 0) return 100;
    
    const withMissing = this.filesWithMissingTags().length;
    return ((total - withMissing) / total) * 100;
  });
}
```

### toSignal() - RxJS → Signals
```typescript
import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MusicFileService } from '../services/music-file.service';

@Component({...})
export class FileListComponent {
  private musicService = inject(MusicFileService);
  
  // Convertir Observable en Signal
  files = toSignal(this.musicService.files$, { initialValue: [] });
  
  // Avec gestion erreur
  scanResult = toSignal(this.musicService.scanInProgress$, {
    initialValue: null,
    requireSync: false
  });
}
```

### effect() - Side effects
```typescript
import { Component, signal, effect } from '@angular/core';

@Component({...})
export class AutoSaveComponent {
  userPreferences = signal({ theme: 'dark', volume: 0.5 });
  
  constructor() {
    // Effect s'exécute quand userPreferences change
    effect(() => {
      const prefs = this.userPreferences();
      localStorage.setItem('preferences', JSON.stringify(prefs));
      console.log('Preferences saved:', prefs);
    });
  }
}
```

### viewChild() avec Signals
```typescript
import { Component, viewChild, ElementRef } from '@angular/core';
import { MatInput } from '@angular/material/input';

@Component({...})
export class SearchComponent {
  // Signal query
  searchInput = viewChild<ElementRef<HTMLInputElement>>('searchInput');
  matInput = viewChild(MatInput);
  
  focusSearch() {
    this.searchInput()?.nativeElement.focus();
  }
  
  clearInput() {
    const input = this.matInput();
    if (input) {
      input.value = '';
    }
  }
}

// Template
// <input #searchInput matInput>
```

### linkedSignal() - Signals liés (Angular 21)
```typescript
import { Component, signal, linkedSignal } from '@angular/core';

@Component({...})
export class PlaylistComponent {
  genre = signal('Techno');
  
  // Signal lié qui se met à jour automatiquement
  recommendedTracks = linkedSignal(() => {
    const currentGenre = this.genre();
    return this.fetchRecommendations(currentGenre);
  });
  
  changeGenre(newGenre: string) {
    this.genre.set(newGenre);
    // recommendedTracks se met à jour automatiquement
  }
}
```

### Signal inputs (Angular 21)
```typescript
import { Component, input, output, computed } from '@angular/core';

@Component({
  selector: 'app-track-item',
  standalone: true,
  template: `
    <div class="track" (click)="onSelect()">
      <h3>{{ track().title }}</h3>
      <p>{{ track().artist }} - {{ bpmInfo() }}</p>
    </div>
  `
})
export class TrackItemComponent {
  // Signal input (Angular 21)
  track = input.required<MusicFile>();
  showBpm = input(true);
  
  // Output
  trackSelected = output<MusicFile>();
  
  // Computed basé sur inputs
  bpmInfo = computed(() => {
    if (!this.showBpm()) return '';
    return `${this.track().bpm} BPM`;
  });
  
  onSelect() {
    this.trackSelected.emit(this.track());
  }
}
```

## Control Flow (@if, @for, @switch)

### @if - Conditionnel
```typescript
@Component({
  template: `
    @if (isLoading()) {
      <mat-spinner></mat-spinner>
    } @else if (error()) {
      <mat-error>{{ error() }}</mat-error>
    } @else {
      <app-file-list [files]="files()"></app-file-list>
    }
  `
})
export class MainComponent {
  isLoading = signal(false);
  error = signal<string | null>(null);
  files = signal<MusicFile[]>([]);
}
```

### @for - Boucles
```typescript
@Component({
  template: `
    <mat-list>
      @for (file of files(); track file.filepath) {
        <mat-list-item>
          {{ file.filename }}
          
          @if (file.missingTags.length > 0) {
            <mat-chip-set>
              @for (tag of file.missingTags; track tag) {
                <mat-chip>{{ tag }}</mat-chip>
              }
            </mat-chip-set>
          }
        </mat-list-item>
      } @empty {
        <p>Aucun fichier sélectionné</p>
      }
    </mat-list>
  `
})
```

### @switch - Switch case
```typescript
@Component({
  template: `
    @switch (mode()) {
      @case ('PLAN') {
        <app-plan-mode></app-plan-mode>
      }
      @case ('MANUAL') {
        <app-manual-mode></app-manual-mode>
      }
      @case ('APPLY') {
        <app-apply-mode></app-apply-mode>
      }
      @default {
        <p>Sélectionnez un mode</p>
      }
    }
  `
})
export class AgentModeComponent {
  mode = signal<'PLAN' | 'MANUAL' | 'APPLY'>('PLAN');
}
```

### @defer - Lazy loading (Angular 21)
```typescript
@Component({
  template: `
    @defer (on viewport) {
      <app-heavy-component></app-heavy-component>
    } @placeholder {
      <div>Chargement...</div>
    } @loading (minimum 500ms) {
      <mat-spinner></mat-spinner>
    } @error {
      <p>Erreur chargement</p>
    }
  `
})
```

## Template
```html
<!-- chat.component.html -->
<div class="chat-container">
  <div class="messages">
    @for (msg of messages(); track msg.timestamp) {
      <mat-card [class.user-message]="msg.role === 'user'"
                [class.assistant-message]="msg.role === 'assistant'">
        <mat-card-content>
          {{ msg.content }}
        </mat-card-content>
        <mat-card-footer>
          <small>{{ msg.timestamp | date:'short' }}</small>
        </mat-card-footer>
      </mat-card>
    }
    
    @if (isLoading()) {
      <mat-spinner diameter="30"></mat-spinner>
    }
  </div>
  
  <div class="input-area">
    <mat-form-field class="full-width">
      <input matInput
             [value]="userInput()"
             (input)="userInput.set($any($event.target).value)"
             (keyup.enter)="sendMessage()"
             placeholder="Message l'agent..."
             [disabled]="isLoading()">
    </mat-form-field>
    
    <button mat-raised-button
            color="primary"
            (click)="sendMessage()"
            [disabled]="!userInput() || isLoading()">
      Envoyer
    </button>
  </div>
</div>
```

## Services Pattern

### HTTP Service
```typescript
// agent.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AgentService {
  private http = inject(HttpClient);
  private baseUrl = 'http://localhost:8080/api/agent';
  
  chat(message: string): Observable<ChatResponse> {
    return this.http.post<ChatResponse>(`${this.baseUrl}/chat`, {
      message
    });
  }
  
  changeMode(mode: AgentMode): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/mode`, { mode });
  }
}

interface ChatResponse {
  message: string;
  suggestions?: TagSuggestions;
  question?: AgentQuestion;
}

type AgentMode = 'PLAN' | 'MANUAL' | 'APPLY';
```

### WebSocket Service
```typescript
// websocket.service.ts
import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class WebSocketService {
  private socket?: WebSocket;
  private messages$ = new Subject<any>();
  
  connect(url: string): Observable<any> {
    this.socket = new WebSocket(url);
    
    this.socket.onmessage = (event) => {
      const data = JSON.parse(event.data);
      this.messages$.next(data);
    };
    
    this.socket.onerror = (error) => {
      console.error('WebSocket error:', error);
    };
    
    return this.messages$.asObservable();
  }
  
  send(message: any): void {
    if (this.socket?.readyState === WebSocket.OPEN) {
      this.socket.send(JSON.stringify(message));
    }
  }
  
  disconnect(): void {
    this.socket?.close();
  }
}
```

### Music File Service
```typescript
// music-file.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class MusicFileService {
  private http = inject(HttpClient);
  private baseUrl = 'http://localhost:8080/api/music';
  
  scanFiles(filepaths: string[]): Observable<ScanResult> {
    return this.http.post<ScanResult>(`${this.baseUrl}/scan`, {
      filepaths
    });
  }
  
  getFileInfo(filepath: string): Observable<MusicFileInfo> {
    return this.http.get<MusicFileInfo>(`${this.baseUrl}/file`, {
      params: { filepath }
    });
  }
}

interface ScanResult {
  files: MusicFileInfo[];
  totalFiles: number;
  filesWithMissingTags: number;
}

interface MusicFileInfo {
  filepath: string;
  filename: string;
  artist?: string;
  title?: string;
  bpm?: string;
  // ...
}
```

## Composants Material

### File List Component
```typescript
// file-list.component.ts
import { Component, signal, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-file-list',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatChipsModule,
    MatIconModule
  ],
  template: `
    <table mat-table [dataSource]="files()">
      <ng-container matColumnDef="filename">
        <th mat-header-cell *matHeaderCellDef>Fichier</th>
        <td mat-cell *matCellDef="let file">{{ file.filename }}</td>
      </ng-container>
      
      <ng-container matColumnDef="artist">
        <th mat-header-cell *matHeaderCellDef>Artiste</th>
        <td mat-cell *matCellDef="let file">
          {{ file.artist || '-' }}
        </td>
      </ng-container>
      
      <ng-container matColumnDef="title">
        <th mat-header-cell *matHeaderCellDef>Titre</th>
        <td mat-cell *matCellDef="let file">
          {{ file.title || '-' }}
        </td>
      </ng-container>
      
      <ng-container matColumnDef="missing">
        <th mat-header-cell *matHeaderCellDef>Tags manquants</th>
        <td mat-cell *matCellDef="let file">
          @for (tag of file.missingTags; track tag) {
            <mat-chip>{{ tag }}</mat-chip>
          }
        </td>
      </ng-container>
      
      <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
      <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
    </table>
  `
})
export class FileListComponent {
  files = input.required<MusicFileInfo[]>();
  displayedColumns = ['filename', 'artist', 'title', 'missing'];
}
```

### Plan Review Component
```typescript
// plan-review.component.ts
import { Component, signal, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';

@Component({
  selector: 'app-plan-review',
  standalone: true,
  imports: [
    CommonModule,
    MatExpansionModule,
    MatButtonModule,
    MatCheckboxModule
  ],
  template: `
    <div class="plan-review">
      <h2>Plan de modifications ({{ plan().operations.length }} fichiers)</h2>
      
      @for (op of plan().operations; track op.filepath) {
        <mat-expansion-panel>
          <mat-expansion-panel-header>
            <mat-panel-title>
              {{ extractFilename(op.filepath) }}
            </mat-panel-title>
            <mat-panel-description>
              {{ countChanges(op) }} modifications
            </mat-panel-description>
          </mat-expansion-panel-header>
          
          <div class="changes">
            @for (change of getChanges(op); track change.field) {
              <div class="change-row">
                <strong>{{ change.field }}:</strong>
                <span class="old">{{ change.old || '(vide)' }}</span>
                →
                <span class="new">{{ change.new }}</span>
              </div>
            }
          </div>
        </mat-expansion-panel>
      }
      
      <div class="actions">
        <button mat-raised-button (click)="onCancel.emit()">
          Annuler
        </button>
        <button mat-raised-button 
                color="primary"
                (click)="onApprove.emit()">
          Approuver et appliquer
        </button>
      </div>
    </div>
  `
})
export class PlanReviewComponent {
  plan = input.required<TaggingPlan>();
  
  onApprove = output<void>();
  onCancel = output<void>();
  
  extractFilename(path: string): string {
    return path.split('/').pop() || path;
  }
  
  countChanges(op: TagOperation): number {
    return Object.keys(op.suggestedTags).length;
  }
  
  getChanges(op: TagOperation) {
    return Object.entries(op.suggestedTags).map(([field, newVal]) => ({
      field,
      old: op.currentTags[field],
      new: newVal
    }));
  }
}
```

### Mode Selector Component
```typescript
// mode-selector.component.ts
import { Component, signal, output } from '@angular/core';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-mode-selector',
  standalone: true,
  imports: [MatButtonToggleModule, MatIconModule],
  template: `
    <mat-button-toggle-group 
      [value]="selectedMode()"
      (change)="onModeChange($event.value)">
      
      <mat-button-toggle value="PLAN">
        <mat-icon>list</mat-icon>
        Plan
      </mat-button-toggle>
      
      <mat-button-toggle value="MANUAL">
        <mat-icon>touch_app</mat-icon>
        Manuel
      </mat-button-toggle>
      
      <mat-button-toggle value="APPLY">
        <mat-icon>play_arrow</mat-icon>
        Auto
      </mat-button-toggle>
    </mat-button-toggle-group>
    
    <div class="mode-description">
      {{ getModeDescription() }}
    </div>
  `
})
export class ModeSelectorComponent {
  selectedMode = signal<AgentMode>('PLAN');
  modeChange = output<AgentMode>();
  
  onModeChange(mode: AgentMode) {
    this.selectedMode.set(mode);
    this.modeChange.emit(mode);
  }
  
  getModeDescription(): string {
    switch (this.selectedMode()) {
      case 'PLAN':
        return 'Génère un plan complet à valider avant application';
      case 'MANUAL':
        return 'Traite fichier par fichier avec confirmation';
      case 'APPLY':
        return 'Applique automatiquement sans confirmation';
      default:
        return '';
    }
  }
}

type AgentMode = 'PLAN' | 'MANUAL' | 'APPLY';
```

## Electron Integration

### Main Process
```typescript
// electron/main.ts
import { app, BrowserWindow, ipcMain, dialog } from 'electron';
import * as path from 'path';

let mainWindow: BrowserWindow;

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1400,
    height: 900,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      preload: path.join(__dirname, 'preload.js')
    }
  });
  
  // File picker pour sélectionner fichiers audio
  ipcMain.handle('select-audio-files', async () => {
    const result = await dialog.showOpenDialog(mainWindow, {
      properties: ['openFile', 'multiSelections'],
      filters: [
        { name: 'Audio Files', extensions: ['mp3', 'flac', 'wav', 'aiff', 'm4a', 'ogg'] }
      ]
    });
    
    return result.filePaths; // Liste des fichiers sélectionnés
  });
  
  if (process.env['NODE_ENV'] === 'development') {
    mainWindow.loadURL('http://localhost:4200');
  } else {
    mainWindow.loadFile(path.join(__dirname, '../dist/index.html'));
  }
}

app.whenReady().then(createWindow);

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});
```

### Preload Script
```typescript
// electron/preload.ts
import { contextBridge, ipcRenderer } from 'electron';

contextBridge.exposeInMainWorld('electron', {
  selectAudioFiles: () => ipcRenderer.invoke('select-audio-files'),
  openFile: (path: string) => ipcRenderer.invoke('open-file', path)
});

// Types pour TypeScript
declare global {
  interface Window {
    electron: {
      selectAudioFiles: () => Promise<string[]>;
      openFile: (path: string) => Promise<void>;
    };
  }
}
```

### Service Angular - File Selection
```typescript
// services/file-selection.service.ts
import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class FileSelectionService {
  selectedFiles = signal<string[]>([]);
  
  async selectFiles(): Promise<string[]> {
    const files = await window.electron.selectAudioFiles();
    this.selectedFiles.set(files);
    return files;
  }
  
  clearSelection(): void {
    this.selectedFiles.set([]);
  }
}
```

### Component - File Picker
```typescript
// components/file-picker.component.ts
import { Component, signal, output, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { FileSelectionService } from '../services/file-selection.service';
import { MusicFileService } from '../services/music-file.service';

@Component({
  selector: 'app-file-picker',
  standalone: true,
  imports: [
    MatButtonModule,
    MatListModule,
    MatIconModule
  ],
  template: `
    <div class="file-picker">
      <button mat-raised-button 
              color="primary"
              (click)="selectFiles()">
        <mat-icon>folder_open</mat-icon>
        Sélectionner fichiers audio
      </button>
      
      @if (selectedFiles().length > 0) {
        <div class="selected-files">
          <h3>{{ selectedFiles().length }} fichiers sélectionnés</h3>
          
          <mat-list>
            @for (file of selectedFiles(); track file) {
              <mat-list-item>
                <mat-icon matListItemIcon>audiotrack</mat-icon>
                <span matListItemTitle>{{ extractFilename(file) }}</span>
                <span matListItemLine>{{ file }}</span>
              </mat-list-item>
            }
          </mat-list>
          
          <div class="actions">
            <button mat-button (click)="clearSelection()">
              Annuler
            </button>
            <button mat-raised-button 
                    color="primary"
                    (click)="scanFiles()"
                    [disabled]="isScanning()">
              Scanner les fichiers
            </button>
          </div>
        </div>
      }
    </div>
  `
})
export class FilePickerComponent {
  // Services avec inject()
  private fileSelectionService = inject(FileSelectionService);
  private musicFileService = inject(MusicFileService);
  
  // Signals
  selectedFiles = this.fileSelectionService.selectedFiles;
  isScanning = signal(false);
  
  // Output
  filesScanned = output<MusicFileInfo[]>();
  
  async selectFiles() {
    await this.fileSelectionService.selectFiles();
  }
  
  clearSelection() {
    this.fileSelectionService.clearSelection();
  }
  
  scanFiles() {
    this.isScanning.set(true);
    
    this.musicFileService.scanFiles(this.selectedFiles()).subscribe({
      next: (result) => {
        this.filesScanned.emit(result.files);
        this.isScanning.set(false);
      },
      error: (err) => {
        console.error('Scan error:', err);
        this.isScanning.set(false);
      }
    });
  }
  
  extractFilename(path: string): string {
    return path.split('/').pop() || path.split('\\').pop() || path;
  }
}

interface MusicFileInfo {
  filepath: string;
  filename: string;
  artist?: string;
  title?: string;
}
```
})
export class FilePickerComponent {
  selectedFiles = this.fileSelectionService.selectedFiles;
  isScanning = signal(false);
  
  filesScanned = output<MusicFileInfo[]>();
  
  constructor(
    private fileSelectionService: FileSelectionService,
    private musicFileService: MusicFileService
  ) {}
  
  async selectFiles() {
    await this.fileSelectionService.selectFiles();
  }
  
  clearSelection() {
    this.fileSelectionService.clearSelection();
  }
  
  scanFiles() {
    this.isScanning.set(true);
    
    this.musicFileService.scanFiles(this.selectedFiles()).subscribe({
      next: (result) => {
        this.filesScanned.emit(result.files);
        this.isScanning.set(false);
      },
      error: (err) => {
        console.error('Scan error:', err);
        this.isScanning.set(false);
      }
    });
  }
  
  extractFilename(path: string): string {
    return path.split('/').pop() || path.split('\\').pop() || path;
  }
}
```

## State Management

### Simple Signal Store
```typescript
// stores/music-store.ts
import { Injectable, signal, computed } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class MusicStore {
  // State
  private _files = signal<MusicFileInfo[]>([]);
  private _currentPlan = signal<TaggingPlan | null>(null);
  private _mode = signal<AgentMode>('PLAN');
  
  // Selectors (computed)
  files = this._files.asReadonly();
  currentPlan = this._currentPlan.asReadonly();
  mode = this._mode.asReadonly();
  
  filesWithMissingTags = computed(() => 
    this._files().filter(f => f.missingTags && f.missingTags.length > 0)
  );
  
  totalMissingTags = computed(() =>
    this._files().reduce((sum, f) => 
      sum + (f.missingTags?.length || 0), 0
    )
  );
  
  // Actions
  setFiles(files: MusicFileInfo[]) {
    this._files.set(files);
  }
  
  setPlan(plan: TaggingPlan) {
    this._currentPlan.set(plan);
  }
  
  setMode(mode: AgentMode) {
    this._mode.set(mode);
  }
  
  clearPlan() {
    this._currentPlan.set(null);
  }
}
```

## Theming

### Material Theme
```scss
// styles.scss
@use '@angular/material' as mat;

$primary: mat.define-palette(mat.$indigo-palette);
$accent: mat.define-palette(mat.$pink-palette);
$theme: mat.define-light-theme((
  color: (
    primary: $primary,
    accent: $accent
  )
));

@include mat.all-component-themes($theme);

// Dark theme
.dark-theme {
  $dark-theme: mat.define-dark-theme((
    color: (
      primary: $primary,
      accent: $accent
    )
  ));
  
  @include mat.all-component-colors($dark-theme);
}
```

## Testing

### Component Test
```typescript
// chat.component.spec.ts
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ChatComponent } from './chat.component';
import { AgentService } from '../services/agent.service';
import { of } from 'rxjs';

describe('ChatComponent', () => {
  let component: ChatComponent;
  let fixture: ComponentFixture<ChatComponent>;
  let agentService: jasmine.SpyObj<AgentService>;
  
  beforeEach(async () => {
    const spy = jasmine.createSpyObj('AgentService', ['chat']);
    
    await TestBed.configureTestingModule({
      imports: [ChatComponent],
      providers: [
        { provide: AgentService, useValue: spy }
      ]
    }).compileComponents();
    
    agentService = TestBed.inject(AgentService) as jasmine.SpyObj<AgentService>;
    fixture = TestBed.createComponent(ChatComponent);
    component = fixture.componentInstance;
  });
  
  it('should send message and update messages signal', () => {
    agentService.chat.and.returnValue(of({ message: 'Response' }));
    
    component.userInput.set('Hello');
    component.sendMessage();
    
    expect(component.messages().length).toBe(2); // user + assistant
    expect(component.userInput()).toBe('');
  });
});
```

## Checklist

- [ ] Standalone components
- [ ] Signals pour state
- [ ] Material components
- [ ] Services pour backend
- [ ] WebSocket temps réel
- [ ] Electron setup
- [ ] Theming dark/light
- [ ] Tests unitaires

## Checklist Angular 21 Modern Patterns

### ✅ Components
- [ ] **Standalone** components (pas de NgModules)
- [ ] **inject()** au lieu de constructor injection
- [ ] **signal()** pour state réactif
- [ ] **computed()** pour valeurs dérivées
- [ ] **input()** pour @Input signal-based
- [ ] **output()** pour @Output
- [ ] **viewChild()** avec signals
- [ ] **effect()** pour side effects

### ✅ Templates
- [ ] **@if / @else** au lieu de *ngIf
- [ ] **@for** au lieu de *ngFor avec @track
- [ ] **@switch / @case** au lieu de *ngSwitch
- [ ] **@defer** pour lazy loading
- [ ] **@empty** dans @for pour cas vide

### ✅ RxJS Integration
- [ ] **toSignal()** pour Observable → Signal
- [ ] Pas de async pipe dans templates (utiliser toSignal à la place)
- [ ] Signals pour state local, RxJS pour events/streams

### ✅ Performance
- [ ] **OnPush** change detection par défaut avec signals
- [ ] **@defer** pour composants lourds
- [ ] **linkedSignal()** pour dépendances signal→signal
- [ ] Éviter mutations directes (utiliser .set() et .update())

### 🚫 À Éviter (Deprecated/Old)
- ❌ **NgModules** (sauf cas spéciaux)
- ❌ **constructor injection** (utiliser inject())
- ❌ ***ngIf / *ngFor** (utiliser @if / @for)
- ❌ **Subject/BehaviorSubject** pour state local (utiliser signals)
- ❌ **CommonModule** import partout (importer directives spécifiques)

## Exemple Complet Angular 21

```typescript
// file-manager.component.ts
import { Component, signal, computed, effect, inject, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatListModule } from '@angular/material/list';
import { MatChipModule } from '@angular/material/chip';
import { MusicFileService } from './services/music-file.service';
import { FileSelectionService } from './services/file-selection.service';

@Component({
  selector: 'app-file-manager',
  standalone: true,
  imports: [MatButtonModule, MatListModule, MatChipModule],
  template: `
    <div class="file-manager">
      <button mat-raised-button color="primary" (click)="selectFiles()">
        Sélectionner fichiers
      </button>
      
      @if (isScanning()) {
        <mat-spinner></mat-spinner>
      } @else if (hasFiles()) {
        <mat-list>
          @for (file of files(); track file.filepath) {
            <mat-list-item>
              <span matListItemTitle>{{ file.filename }}</span>
              
              @if (file.missingTags.length > 0) {
                <mat-chip-set matListItemMeta>
                  @for (tag of file.missingTags; track tag) {
                    <mat-chip color="warn">{{ tag }}</mat-chip>
                  }
                </mat-chip-set>
              }
            </mat-list-item>
          } @empty {
            <p>Aucun fichier</p>
          }
        </mat-list>
        
        <div class="stats">
          <p>Total: {{ totalFiles() }}</p>
          <p>Avec tags manquants: {{ filesWithIssues() }}</p>
          <p>Complétion: {{ completionRate() }}%</p>
        </div>
      }
    </div>
  `
})
export class FileManagerComponent {
  // Services avec inject()
  private musicService = inject(MusicFileService);
  private fileSelection = inject(FileSelectionService);
  
  // Signals
  files = signal<MusicFile[]>([]);
  isScanning = signal(false);
  
  // Computed signals
  totalFiles = computed(() => this.files().length);
  filesWithIssues = computed(() => 
    this.files().filter(f => f.missingTags.length > 0).length
  );
  completionRate = computed(() => {
    const total = this.totalFiles();
    if (total === 0) return 100;
    return Math.round(((total - this.filesWithIssues()) / total) * 100);
  });
  hasFiles = computed(() => this.totalFiles() > 0);
  
  // Effect pour auto-save
  constructor() {
    effect(() => {
      const rate = this.completionRate();
      console.log(`Completion: ${rate}%`);
      if (rate === 100) {
        console.log('Tous les fichiers sont complets !');
      }
    });
  }
  
  async selectFiles() {
    const selected = await this.fileSelection.selectFiles();
    
    this.isScanning.set(true);
    this.musicService.scanFiles(selected).subscribe({
      next: (result) => {
        this.files.set(result.files);
        this.isScanning.set(false);
      },
      error: () => this.isScanning.set(false)
    });
  }
}

interface MusicFile {
  filepath: string;
  filename: string;
  missingTags: string[];
}
```
