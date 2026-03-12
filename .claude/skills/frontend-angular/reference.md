# Frontend Angular 21 — Reference Rapide

## Signal API Complet

| Fonction | Usage | Exemple |
|----------|-------|---------|
| `signal<T>(initial)` | State reactif mutable | `count = signal(0)` |
| `computed(() => expr)` | Valeur derivee (read-only) | `double = computed(() => this.count() * 2)` |
| `effect(() => {...})` | Side effect sur changement | `effect(() => console.log(this.count()))` |
| `input<T>()` | Input signal (optionnel) | `label = input('')` |
| `input.required<T>()` | Input signal (obligatoire) | `track = input.required<Track>()` |
| `output<T>()` | Output signal | `selected = output<Track>()` |
| `model<T>()` | Two-way binding signal | `value = model('')` |
| `toSignal(obs$)` | Observable → Signal | `data = toSignal(this.http.get(...))` |
| `toObservable(sig)` | Signal → Observable | `count$ = toObservable(this.count)` |
| `.set(value)` | Remplacer valeur | `this.count.set(5)` |
| `.update(fn)` | Transformer valeur | `this.count.update(n => n + 1)` |
| `.asReadonly()` | Signal read-only | `public count = this._count.asReadonly()` |

## Control Flow — Cheat Sheet

```html
<!-- @if / @else if / @else -->
@if (loading()) {
  <spinner />
} @else if (error()) {
  <error-msg [text]="error()" />
} @else {
  <content />
}

<!-- @for avec @track (OBLIGATOIRE) et @empty -->
@for (item of items(); track item.id) {
  <item-card [data]="item" />
} @empty {
  <p>Aucun element</p>
}

<!-- @switch / @case / @default -->
@switch (mode()) {
  @case ('PLAN') { <plan-view /> }
  @case ('MANUAL') { <manual-view /> }
  @default { <default-view /> }
}

<!-- @defer (lazy loading) -->
@defer (on viewport) {
  <heavy-component />
} @placeholder {
  <skeleton />
} @loading (minimum 300ms) {
  <spinner />
}
```

## Material 21 — Imports Frequents

| Module | Composants |
|--------|------------|
| `MatButtonModule` | `mat-button`, `mat-raised-button`, `mat-icon-button`, `mat-fab` |
| `MatInputModule` | `mat-form-field`, `matInput` |
| `MatCardModule` | `mat-card`, `mat-card-header`, `mat-card-content`, `mat-card-actions` |
| `MatListModule` | `mat-list`, `mat-list-item`, `mat-nav-list` |
| `MatTableModule` | `mat-table`, `matColumnDef`, `mat-header-row`, `mat-row` |
| `MatChipsModule` | `mat-chip-set`, `mat-chip` |
| `MatDialogModule` | `mat-dialog-title`, `mat-dialog-content`, `mat-dialog-actions` |
| `MatSidenavModule` | `mat-sidenav-container`, `mat-sidenav`, `mat-sidenav-content` |
| `MatToolbarModule` | `mat-toolbar` |
| `MatIconModule` | `mat-icon` |
| `MatProgressBarModule` | `mat-progress-bar` |
| `MatProgressSpinnerModule` | `mat-spinner` |
| `MatSnackBarModule` | `MatSnackBar` (service) |
| `MatTooltipModule` | `matTooltip` |
| `MatExpansionModule` | `mat-expansion-panel`, `mat-accordion` |
| `MatButtonToggleModule` | `mat-button-toggle-group`, `mat-button-toggle` |

## Services Existants et Endpoints

| Service | Endpoint Backend | Methodes |
|---------|-----------------|----------|
| `AgentService` | `/api/agent` | `chat(msg)`, `getHistory(id)`, `clearConversation(id)` |
| `PlanService` | `/api/plans` | `create(files)`, `get(id)`, `approve(id)`, `execute(id)`, `preview(id)` |
| `TagsService` | `/api/tags` | `apply(filepath, tags)`, `preview(filepath, tags)` |
| `RagService` | `/api/rag` | `findSimilar(query, limit)` |
| `WebSocketService` | `/ws` (STOMP) | `connect()`, `send(msg)`, `disconnect()` |
| `FileSelectionService` | Electron IPC | `selectFiles()`, `selectedFiles` (signal) |
| `ModeService` | Local (localStorage) | `currentMode`, `setMode(mode)` |
| `StatsService` | `/api/stats` | `getStats()` |

## Patterns Deprecated — A Eviter

| Deprecated | Remplacement Angular 21 |
|-----------|------------------------|
| `NgModules` | Standalone components |
| `*ngIf` | `@if` |
| `*ngFor` | `@for` avec `track` |
| `*ngSwitch` | `@switch` |
| `constructor(private svc: Service)` | `svc = inject(Service)` |
| `@Input() prop: Type` | `prop = input<Type>()` |
| `@Output() event = new EventEmitter()` | `event = output<Type>()` |
| `BehaviorSubject` (state local) | `signal()` |
| `async` pipe (state local) | `toSignal()` + direct read |

## Routes (Lazy-loaded)

```typescript
export const routes: Routes = [
  { path: '', component: ChatComponent },
  { path: 'plan/:id', loadComponent: () => import('./pages/plan-review/plan-review') },
  { path: 'history', loadComponent: () => import('./pages/history/history') },
  { path: 'settings', loadComponent: () => import('./pages/settings/settings') },
  { path: 'stats', loadComponent: () => import('./pages/stats/stats') },
];
```
