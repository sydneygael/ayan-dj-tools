---
name: frontend-angular
description: Frontend Angular 21 + Material 21 + Electron 40. Standalone components, signals, control flow (@if/@for/@switch), inject(), services HTTP/WebSocket, theming Material 3. Utiliser pour tout code frontend Angular.
user-invocable: false
---

# Frontend Angular Skill

Angular 21 + Material + Electron

> Exemples composants Material : voir [components.md](./components.md)
> Integration Electron : voir [electron.md](./electron.md)
> Reference rapide API : voir [reference.md](./reference.md)

## Principes Angular 21

- **Standalone components** (par defaut, pas NgModules)
- **Signals** pour reactivite (signal, computed, effect)
- **inject()** au lieu de constructor injection
- **input() / output()** pour component API
- **Control flow** (@if, @for, @switch au lieu de *ngIf, *ngFor)
- **toSignal()** pour RxJS -> Signals
- **Material 21** pour UI
- **WebSocket** pour temps reel

## Setup Projet

```typescript
// app.config.ts
import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideHttpClient } from '@angular/common/http';

export const appConfig: ApplicationConfig = {
  providers: [provideRouter(routes), provideAnimations(), provideHttpClient()]
};
```

## Standalone Component Pattern

```typescript
@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [MatButtonModule, MatInputModule, MatCardModule],
  templateUrl: './chat.component.html'
})
export class ChatComponent {
  private agentService = inject(AgentService);

  messages = signal<Message[]>([]);
  userInput = signal('');
  isLoading = signal(false);
  messageCount = computed(() => this.messages().length);

  constructor() {
    effect(() => console.log(`Messages: ${this.messageCount()}`));
  }

  sendMessage() {
    const content = this.userInput();
    if (!content.trim()) return;
    this.messages.update(msgs => [...msgs, { role: 'user', content, timestamp: new Date() }]);
    this.userInput.set('');
    this.isLoading.set(true);

    this.agentService.chat(content).subscribe({
      next: (response) => {
        this.messages.update(msgs => [...msgs,
          { role: 'assistant', content: response.message, timestamp: new Date() }]);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false)
    });
  }
}
```

## Signals Avances

### computed() - Valeurs derivees
```typescript
files = signal<MusicFile[]>([]);
totalFiles = computed(() => this.files().length);
filesWithMissingTags = computed(() =>
  this.files().filter(f => f.missingTags.length > 0));
completionRate = computed(() => {
  const total = this.totalFiles();
  if (total === 0) return 100;
  return ((total - this.filesWithMissingTags().length) / total) * 100;
});
```

### toSignal() - RxJS -> Signals
```typescript
import { toSignal } from '@angular/core/rxjs-interop';

files = toSignal(this.musicService.files$, { initialValue: [] });
```

### Signal inputs (Angular 21)
```typescript
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
  track = input.required<MusicFile>();
  showBpm = input(true);
  trackSelected = output<MusicFile>();
  bpmInfo = computed(() => this.showBpm() ? `${this.track().bpm} BPM` : '');
  onSelect() { this.trackSelected.emit(this.track()); }
}
```

## Control Flow (@if, @for, @switch)

### @if
```html
@if (isLoading()) {
  <mat-spinner></mat-spinner>
} @else if (error()) {
  <mat-error>{{ error() }}</mat-error>
} @else {
  <app-file-list [files]="files()"></app-file-list>
}
```

### @for
```html
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
    <p>Aucun fichier selectionne</p>
  }
</mat-list>
```

### @switch
```html
@switch (mode()) {
  @case ('PLAN') { <app-plan-mode></app-plan-mode> }
  @case ('MANUAL') { <app-manual-mode></app-manual-mode> }
  @case ('APPLY') { <app-apply-mode></app-apply-mode> }
  @default { <p>Selectionnez un mode</p> }
}
```

### @defer
```html
@defer (on viewport) {
  <app-heavy-component></app-heavy-component>
} @placeholder { <div>Chargement...</div> }
```

## Services Pattern

### HTTP Service
```typescript
@Injectable({ providedIn: 'root' })
export class AgentService {
  private http = inject(HttpClient);
  private baseUrl = 'http://localhost:8080/api/agent';

  chat(message: string): Observable<ChatResponse> {
    return this.http.post<ChatResponse>(`${this.baseUrl}/chat`, { message });
  }

  changeMode(mode: AgentMode): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/mode`, { mode });
  }
}
```

### WebSocket Service
```typescript
@Injectable({ providedIn: 'root' })
export class WebSocketService {
  private socket?: WebSocket;
  private messages$ = new Subject<any>();

  connect(url: string): Observable<any> {
    this.socket = new WebSocket(url);
    this.socket.onmessage = (event) => this.messages$.next(JSON.parse(event.data));
    return this.messages$.asObservable();
  }

  send(message: any): void {
    if (this.socket?.readyState === WebSocket.OPEN)
      this.socket.send(JSON.stringify(message));
  }

  disconnect(): void { this.socket?.close(); }
}
```

## State Management

```typescript
@Injectable({ providedIn: 'root' })
export class MusicStore {
  private _files = signal<MusicFileInfo[]>([]);
  private _currentPlan = signal<TaggingPlan | null>(null);
  private _mode = signal<AgentMode>('PLAN');

  files = this._files.asReadonly();
  currentPlan = this._currentPlan.asReadonly();
  mode = this._mode.asReadonly();

  filesWithMissingTags = computed(() =>
    this._files().filter(f => f.missingTags && f.missingTags.length > 0));

  setFiles(files: MusicFileInfo[]) { this._files.set(files); }
  setPlan(plan: TaggingPlan) { this._currentPlan.set(plan); }
  setMode(mode: AgentMode) { this._mode.set(mode); }
}
```

## Theming

```scss
@use '@angular/material' as mat;

$primary: mat.define-palette(mat.$indigo-palette);
$accent: mat.define-palette(mat.$pink-palette);
$theme: mat.define-light-theme((color: (primary: $primary, accent: $accent)));
@include mat.all-component-themes($theme);

.dark-theme {
  $dark-theme: mat.define-dark-theme((color: (primary: $primary, accent: $accent)));
  @include mat.all-component-colors($dark-theme);
}
```

## Checklist Angular 21

### Components
- [ ] **Standalone** components (pas de NgModules)
- [ ] **inject()** au lieu de constructor injection
- [ ] **signal()** pour state reactif
- [ ] **computed()** pour valeurs derivees
- [ ] **input()** pour @Input signal-based
- [ ] **output()** pour @Output

### Templates
- [ ] **@if / @else** au lieu de *ngIf
- [ ] **@for** au lieu de *ngFor avec @track
- [ ] **@switch / @case** au lieu de *ngSwitch
- [ ] **@defer** pour lazy loading

### A Eviter (Deprecated/Old)
- NgModules (sauf cas speciaux)
- constructor injection (utiliser inject())
- *ngIf / *ngFor (utiliser @if / @for)
- Subject/BehaviorSubject pour state local (utiliser signals)
