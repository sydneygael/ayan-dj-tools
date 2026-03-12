# Frontend Angular — Material Components

## File List Component

```typescript
@Component({
  selector: 'app-file-list',
  standalone: true,
  imports: [MatTableModule, MatChipsModule, MatIconModule],
  template: `
    <table mat-table [dataSource]="files()">
      <ng-container matColumnDef="filename">
        <th mat-header-cell *matHeaderCellDef>Fichier</th>
        <td mat-cell *matCellDef="let file">{{ file.filename }}</td>
      </ng-container>
      <ng-container matColumnDef="artist">
        <th mat-header-cell *matHeaderCellDef>Artiste</th>
        <td mat-cell *matCellDef="let file">{{ file.artist || '-' }}</td>
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

## Plan Review Component

```typescript
@Component({
  selector: 'app-plan-review',
  standalone: true,
  imports: [MatExpansionModule, MatButtonModule, MatCheckboxModule],
  template: `
    <div class="plan-review">
      <h2>Plan de modifications ({{ plan().operations.length }} fichiers)</h2>
      @for (op of plan().operations; track op.filepath) {
        <mat-expansion-panel>
          <mat-expansion-panel-header>
            <mat-panel-title>{{ extractFilename(op.filepath) }}</mat-panel-title>
            <mat-panel-description>{{ countChanges(op) }} modifications</mat-panel-description>
          </mat-expansion-panel-header>
          <div class="changes">
            @for (change of getChanges(op); track change.field) {
              <div class="change-row">
                <strong>{{ change.field }}:</strong>
                <span class="old">{{ change.old || '(vide)' }}</span> ->
                <span class="new">{{ change.new }}</span>
              </div>
            }
          </div>
        </mat-expansion-panel>
      }
      <div class="actions">
        <button mat-raised-button (click)="onCancel.emit()">Annuler</button>
        <button mat-raised-button color="primary" (click)="onApprove.emit()">
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

  extractFilename(path: string): string { return path.split('/').pop() || path; }
  countChanges(op: TagOperation): number { return Object.keys(op.suggestedTags).length; }
  getChanges(op: TagOperation) {
    return Object.entries(op.suggestedTags).map(([field, newVal]) => ({
      field, old: op.currentTags[field], new: newVal
    }));
  }
}
```

## Mode Selector Component

```typescript
@Component({
  selector: 'app-mode-selector',
  standalone: true,
  imports: [MatButtonToggleModule, MatIconModule],
  template: `
    <mat-button-toggle-group [value]="selectedMode()" (change)="onModeChange($event.value)">
      <mat-button-toggle value="PLAN">
        <mat-icon>list</mat-icon> Plan
      </mat-button-toggle>
      <mat-button-toggle value="MANUAL">
        <mat-icon>touch_app</mat-icon> Manuel
      </mat-button-toggle>
      <mat-button-toggle value="APPLY">
        <mat-icon>play_arrow</mat-icon> Auto
      </mat-button-toggle>
    </mat-button-toggle-group>
  `
})
export class ModeSelectorComponent {
  selectedMode = signal<AgentMode>('PLAN');
  modeChange = output<AgentMode>();

  onModeChange(mode: AgentMode) {
    this.selectedMode.set(mode);
    this.modeChange.emit(mode);
  }
}
```

## File Manager (Exemple Complet Angular 21)

```typescript
@Component({
  selector: 'app-file-manager',
  standalone: true,
  imports: [MatButtonModule, MatListModule, MatChipModule],
  template: `
    <div class="file-manager">
      <button mat-raised-button color="primary" (click)="selectFiles()">
        Selectionner fichiers
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
          } @empty { <p>Aucun fichier</p> }
        </mat-list>
        <div class="stats">
          <p>Total: {{ totalFiles() }} | Avec tags manquants: {{ filesWithIssues() }}</p>
          <p>Completion: {{ completionRate() }}%</p>
        </div>
      }
    </div>
  `
})
export class FileManagerComponent {
  private musicService = inject(MusicFileService);
  private fileSelection = inject(FileSelectionService);

  files = signal<MusicFile[]>([]);
  isScanning = signal(false);
  totalFiles = computed(() => this.files().length);
  filesWithIssues = computed(() =>
    this.files().filter(f => f.missingTags.length > 0).length);
  completionRate = computed(() => {
    const total = this.totalFiles();
    if (total === 0) return 100;
    return Math.round(((total - this.filesWithIssues()) / total) * 100);
  });
  hasFiles = computed(() => this.totalFiles() > 0);

  async selectFiles() {
    const selected = await this.fileSelection.selectFiles();
    this.isScanning.set(true);
    this.musicService.scanFiles(selected).subscribe({
      next: (result) => { this.files.set(result.files); this.isScanning.set(false); },
      error: () => this.isScanning.set(false)
    });
  }
}
```

## Testing

```typescript
describe('ChatComponent', () => {
  let component: ChatComponent;
  let fixture: ComponentFixture<ChatComponent>;
  let agentService: jasmine.SpyObj<AgentService>;

  beforeEach(async () => {
    const spy = jasmine.createSpyObj('AgentService', ['chat']);
    await TestBed.configureTestingModule({
      imports: [ChatComponent],
      providers: [{ provide: AgentService, useValue: spy }]
    }).compileComponents();

    agentService = TestBed.inject(AgentService) as jasmine.SpyObj<AgentService>;
    fixture = TestBed.createComponent(ChatComponent);
    component = fixture.componentInstance;
  });

  it('should send message and update messages signal', () => {
    agentService.chat.and.returnValue(of({ message: 'Response' }));
    component.userInput.set('Hello');
    component.sendMessage();
    expect(component.messages().length).toBe(2);
  });
});
```
