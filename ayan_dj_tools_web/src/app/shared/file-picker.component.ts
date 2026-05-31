import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { ApiService } from '../core/api.service';
import { FileBrowserPage } from '../core/models';
import { PreferencesStore } from '../core/preferences.store';

interface Breadcrumb {
  label: string;
  path: string;
}

@Component({
  standalone: true,
  selector: 'app-file-picker',
  imports: [
    CommonModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
  ],
  template: `
    <div class="path-row">
      <mat-form-field subscriptSizing="dynamic" class="path-field">
        <mat-label>Chemin du dossier</mat-label>
        <input
          matInput
          [value]="pathInput()"
          (input)="pathInput.set($any($event.target).value)"
          placeholder="C:\\Music ou /home/user/music"
          (keyup.enter)="browse(pathInput())"
        />
        <button mat-icon-button matSuffix (click)="browse(pathInput())" [disabled]="loading()"
                aria-label="Parcourir le dossier">
          <mat-icon fontSet="material-symbols-rounded">{{ browsePage() ? 'folder_open' : 'folder' }}</mat-icon>
        </button>
      </mat-form-field>
    </div>

    @if (!browsePage() && !error()) {
      <p class="hint">Clique sur <mat-icon class="hint-icon" fontSet="material-symbols-rounded">folder</mat-icon> ou saisis un chemin (ex. <code>C:\\Music</code>) pour naviguer.</p>
    }

    @if (error()) {
      <p class="error">{{ error() }}</p>
    }

    @if (browsePage()) {
      <div class="browser">
        <nav class="breadcrumb">
          @for (crumb of breadcrumbs(); track crumb.path; let last = $last) {
            @if (!last) {
              <button mat-button class="crumb-btn" (click)="browse(crumb.path)">{{ crumb.label }}</button>
              <mat-icon class="crumb-sep" fontSet="material-symbols-rounded">chevron_right</mat-icon>
            } @else {
              <span class="crumb-current">{{ crumb.label }}</span>
            }
          }
        </nav>

        <table class="file-table">
          <thead>
            <tr>
              <th class="col-check"></th>
              <th>Nom</th>
              <th>Artiste</th>
              <th>Titre</th>
              <th>Tags</th>
              <th>Taille</th>
            </tr>
          </thead>
          <tbody>
            @for (entry of browsePage()!.entries; track entry.absolutePath) {
              <tr [class.dir-row]="entry.isDirectory">
                <td class="col-check">
                  @if (!entry.isDirectory) {
                    <input
                      type="checkbox"
                      [checked]="selected().has(entry.absolutePath)"
                      (change)="toggle(entry.absolutePath)"
                    />
                  }
                </td>
                <td>
                  @if (entry.isDirectory) {
                    <button mat-button class="folder-btn" (click)="browse(entry.absolutePath)">
                      <mat-icon fontSet="material-symbols-rounded">folder</mat-icon>
                      {{ entry.name }}
                    </button>
                  } @else {
                    <span class="mono">
                      <mat-icon class="file-icon" fontSet="material-symbols-rounded">audio_file</mat-icon>
                      {{ entry.name }}
                    </span>
                  }
                </td>
                <td>{{ entry.artist || '—' }}</td>
                <td>{{ entry.title || '—' }}</td>
                <td>
                  @if (!entry.isDirectory) {
                    <mat-icon
                      class="tag-icon"
                      [class.ok]="entry.hasCompleteTags"
                      [class.ko]="!entry.hasCompleteTags"
                      fontSet="material-symbols-rounded"
                    >
                      {{ entry.hasCompleteTags ? 'check_circle' : 'cancel' }}
                    </mat-icon>
                  }
                </td>
                <td class="mono size-col">
                  @if (!entry.isDirectory) { {{ formatSize(entry.fileSizeBytes) }} }
                </td>
              </tr>
            }
            @if (browsePage()!.entries.length === 0) {
              <tr>
                <td colspan="6" class="empty-cell">Dossier vide.</td>
              </tr>
            }
          </tbody>
        </table>

        <div class="browser-footer">
          <div class="footer-left">
            <button mat-button (click)="selectAllOnPage()">Tout sélectionner</button>
            <button mat-button (click)="deselectAll()">Désélectionner</button>
            <span class="small">{{ selectedCount() }} sélectionné(s)</span>
          </div>
          <div class="footer-right">
            <button mat-icon-button (click)="prevPage()" [disabled]="currentPageNum === 0">
              <mat-icon fontSet="material-symbols-rounded">chevron_left</mat-icon>
            </button>
            <span class="small">Page {{ currentPageNum + 1 }} / {{ browsePage()!.totalPages }}</span>
            <button mat-icon-button (click)="nextPage()" [disabled]="currentPageNum >= browsePage()!.totalPages - 1">
              <mat-icon fontSet="material-symbols-rounded">chevron_right</mat-icon>
            </button>
          </div>
        </div>
      </div>

      @if (selectedCount() > 0) {
        <div class="confirm-row">
          <button mat-flat-button (click)="confirm()">
            <mat-icon fontSet="material-symbols-rounded">task_alt</mat-icon>
            Valider la sélection ({{ selectedCount() }})
          </button>
        </div>
      }
    }
  `,
  styles: `
    .path-row {
      display: flex;
      gap: 8px;
      align-items: flex-start;
      margin-bottom: 4px;
    }

    .path-field { flex: 1; }

    .hint {
      font-size: .82rem;
      color: var(--muted);
      margin: 4px 2px 0;
    }

    .hint code {
      background: var(--surface-1);
      padding: 1px 4px;
      border-radius: 3px;
      font-size: .8rem;
    }

    .hint-icon {
      font-size: 14px;
      width: 14px;
      height: 14px;
      vertical-align: middle;
      color: var(--accent-text);
    }

    .browser {
      border: 1px solid var(--border);
      border-radius: 8px;
      overflow: hidden;
      margin-top: 8px;
    }

    .breadcrumb {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 0;
      padding: 4px 8px;
      background: var(--surface-0);
      border-bottom: 1px solid var(--border);
      font-size: .85rem;
      min-height: 36px;
    }

    .crumb-btn {
      font-size: .83rem !important;
      height: 28px !important;
      min-width: unset !important;
      padding: 0 4px !important;
      color: var(--accent-text) !important;
    }

    .crumb-sep {
      color: var(--muted);
      font-size: 18px;
      width: 18px;
      height: 18px;
    }

    .crumb-current {
      color: var(--text);
      font-weight: 500;
      font-size: .83rem;
      padding: 0 4px;
    }

    .file-table {
      width: 100%;
      border-collapse: collapse;
      font-size: .88rem;
    }

    .file-table th {
      border-bottom: 1px solid var(--border);
      padding: 6px 8px;
      text-align: left;
      font-size: .78rem;
      color: var(--muted);
      text-transform: uppercase;
      letter-spacing: .04em;
    }

    .file-table td {
      border-bottom: 1px solid var(--border);
      padding: 6px 8px;
      vertical-align: middle;
    }

    .file-table tbody tr:last-child td { border-bottom: none; }

    .file-table tbody tr:hover { background: var(--surface-2); }

    .col-check { width: 32px; }

    .dir-row { color: var(--muted); }

    .folder-btn {
      font-size: .87rem !important;
      height: 28px !important;
      min-width: unset !important;
      padding: 0 4px !important;
      color: var(--accent-text) !important;
      font-weight: 500;
      display: flex;
      align-items: center;
      gap: 4px;
    }

    .file-icon {
      font-size: 16px;
      width: 16px;
      height: 16px;
      color: var(--muted);
      vertical-align: middle;
      margin-right: 4px;
    }

    .tag-icon {
      font-size: 18px;
      width: 18px;
      height: 18px;
      &.ok { color: var(--success); }
      &.ko { color: var(--muted); }
    }

    .size-col { color: var(--muted); white-space: nowrap; }

    .empty-cell {
      padding: 16px !important;
      text-align: center;
      color: var(--muted);
      font-size: .9rem;
    }

    .browser-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 4px 8px;
      background: var(--surface-0);
      border-top: 1px solid var(--border);
    }

    .footer-left, .footer-right {
      display: flex;
      align-items: center;
      gap: 4px;
    }

    .confirm-row {
      display: flex;
      justify-content: flex-end;
      margin-top: 8px;
    }

    input[type="checkbox"] {
      width: 16px;
      height: 16px;
      cursor: pointer;
      accent-color: var(--accent-text);
    }
  `
})
export class FilePickerComponent {
  private readonly api = inject(ApiService);
  private readonly prefs = inject(PreferencesStore);

  readonly browsePage = signal<FileBrowserPage | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly selected = signal<Set<string>>(new Set());
  readonly filesSelected = output<string[]>();

  readonly pathInput = signal(this.prefs.currentDir() ?? this.prefs.defaultMusicDir() ?? '');
  currentPageNum = 0;

  readonly breadcrumbs = computed((): Breadcrumb[] => {
    const dir = this.browsePage()?.directory ?? '';
    const normalized = dir.replace(/\\/g, '/');
    const parts = normalized.split('/').filter(Boolean);
    return parts.map((seg, i) => {
      let path = parts.slice(0, i + 1).join('/');
      if (/^[a-zA-Z]:$/.test(parts[0]) && i === 0) {
        path = `${parts[0]}/`;
      } else if (/^[a-zA-Z]:$/.test(parts[0])) {
        path = `${parts[0]}/${parts.slice(1, i + 1).join('/')}`;
      }
      return { label: seg, path };
    });
  });

  readonly selectedCount = computed(() => this.selected().size);

  browse(path: string, page = 0): void {
    this.loading.set(true);
    this.error.set(null);
    this.currentPageNum = page;
    this.api.browsePath(path, page).subscribe({
      next: (result) => {
        this.browsePage.set(result);
        this.pathInput.set(result.directory);
        this.prefs.setCurrentDir(result.directory);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        const detail = err.error?.detail ?? err.error?.message ?? err.statusText;
        this.error.set(detail ?? `Erreur ${err.status} — vérifiez le chemin`);
        this.loading.set(false);
      }
    });
  }

  toggle(path: string): void {
    const s = new Set(this.selected());
    s.has(path) ? s.delete(path) : s.add(path);
    this.selected.set(s);
  }

  selectAllOnPage(): void {
    const s = new Set(this.selected());
    this.browsePage()?.entries
      .filter((e) => !e.isDirectory)
      .forEach((e) => s.add(e.absolutePath));
    this.selected.set(s);
  }

  deselectAll(): void {
    this.selected.set(new Set());
  }

  confirm(): void {
    const paths = [...this.selected()];
    this.prefs.setSelectedFiles(paths);
    this.filesSelected.emit(paths);
  }

  nextPage(): void {
    const p = this.browsePage();
    if (p && this.currentPageNum < p.totalPages - 1) {
      this.browse(p.directory, this.currentPageNum + 1);
    }
  }

  prevPage(): void {
    if (this.currentPageNum > 0) {
      this.browse(this.browsePage()!.directory, this.currentPageNum - 1);
    }
  }

  formatSize(bytes: number): string {
    if (bytes > 1_000_000) return `${(bytes / 1_000_000).toFixed(1)} MB`;
    return `${(bytes / 1000).toFixed(0)} KB`;
  }
}
