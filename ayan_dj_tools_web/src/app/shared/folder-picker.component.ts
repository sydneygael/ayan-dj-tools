import { Component, computed, inject, input, output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiService } from '../core/api.service';
import { FileBrowserPage } from '../core/models';

interface Breadcrumb { label: string; path: string; }

@Component({
  standalone: true,
  selector: 'app-folder-picker',
  imports: [MatButtonModule, MatFormFieldModule, MatInputModule, MatIconModule],
  template: `
    <div class="row path-row">
      <mat-form-field subscriptSizing="dynamic" class="path-field">
        <mat-label>{{ label() }}</mat-label>
        <input
          matInput
          [value]="pathInput()"
          (input)="pathInput.set($any($event.target).value)"
          [placeholder]="placeholder()"
          (keyup.enter)="open(pathInput())"
        />
        <mat-icon matSuffix fontSet="material-symbols-rounded">folder</mat-icon>
      </mat-form-field>

      @if (!showBrowser()) {
        <button mat-stroked-button (click)="open(pathInput())" [disabled]="loading()">
          <mat-icon fontSet="material-symbols-rounded">folder_open</mat-icon>
          Parcourir
        </button>
      } @else {
        <button mat-button (click)="closeBrowser()">
          <mat-icon fontSet="material-symbols-rounded">close</mat-icon>
          Fermer
        </button>
      }
    </div>

    @if (error()) {
      <p class="error">{{ error() }}</p>
    }

    @if (showBrowser() && browsePage()) {
      <div class="browser">
        <nav class="breadcrumb">
          @for (crumb of breadcrumbs(); track crumb.path; let last = $last) {
            @if (!last) {
              <button mat-button class="crumb-btn" (click)="open(crumb.path)">{{ crumb.label }}</button>
              <mat-icon class="crumb-sep" fontSet="material-symbols-rounded">chevron_right</mat-icon>
            } @else {
              <span class="crumb-current">{{ crumb.label }}</span>
            }
          }
        </nav>

        <div class="dir-list">
          @if (dirs().length === 0) {
            <p class="empty-dirs">Aucun sous-dossier.</p>
          }
          @for (entry of dirs(); track entry.absolutePath) {
            <button mat-button class="dir-row" (click)="open(entry.absolutePath)">
              <mat-icon fontSet="material-symbols-rounded">folder</mat-icon>
              <span class="dir-name">{{ entry.name }}</span>
              <mat-icon class="dir-arrow" fontSet="material-symbols-rounded">chevron_right</mat-icon>
            </button>
          }
        </div>

        <div class="browser-footer">
          <button mat-flat-button (click)="select()">
            <mat-icon fontSet="material-symbols-rounded">check_circle</mat-icon>
            Sélectionner ce dossier
          </button>
          <div class="pagination">
            <button mat-icon-button (click)="prevPage()" [disabled]="currentPage === 0 || loading()">
              <mat-icon fontSet="material-symbols-rounded">chevron_left</mat-icon>
            </button>
            <span class="small">{{ currentPage + 1 }} / {{ browsePage()!.totalPages }}</span>
            <button mat-icon-button (click)="nextPage()" [disabled]="currentPage >= browsePage()!.totalPages - 1 || loading()">
              <mat-icon fontSet="material-symbols-rounded">chevron_right</mat-icon>
            </button>
          </div>
        </div>
      </div>
    }
  `,
  styles: `
    .path-row { gap: 8px; align-items: flex-start; }
    .path-field { flex: 1; }

    .browser {
      margin-top: 6px;
      border: 1px solid var(--border);
      border-radius: 8px;
      overflow: hidden;
    }

    .breadcrumb {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 0;
      padding: 4px 8px;
      background: var(--surface-0);
      border-bottom: 1px solid var(--border);
      min-height: 36px;
    }

    .crumb-btn {
      font-size: .82rem !important;
      height: 28px !important;
      min-width: unset !important;
      padding: 0 4px !important;
      color: var(--accent-text) !important;
    }

    .crumb-sep { color: var(--muted); font-size: 18px; width: 18px; height: 18px; }

    .crumb-current {
      color: var(--text);
      font-weight: 500;
      font-size: .82rem;
      padding: 0 4px;
    }

    .dir-list {
      display: flex;
      flex-direction: column;
      max-height: 220px;
      overflow-y: auto;
    }

    .dir-row {
      display: flex !important;
      align-items: center !important;
      gap: 6px !important;
      text-align: left !important;
      height: 36px !important;
      padding: 0 12px !important;
      border-radius: 0 !important;
      border-bottom: 1px solid var(--border) !important;
      font-size: .88rem !important;
      color: var(--text) !important;
      justify-content: flex-start !important;

      &:last-child { border-bottom: none !important; }
      &:hover { background: var(--surface-2) !important; }

      mat-icon:first-child { color: var(--accent-text); font-size: 18px; width: 18px; height: 18px; flex-shrink: 0; }
    }

    .dir-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

    .dir-arrow { color: var(--muted); font-size: 16px; width: 16px; height: 16px; flex-shrink: 0; }

    .empty-dirs {
      padding: 12px 16px;
      color: var(--muted);
      font-size: .88rem;
      margin: 0;
    }

    .browser-footer {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 6px 8px;
      background: var(--surface-0);
      border-top: 1px solid var(--border);
    }

    .pagination {
      display: flex;
      align-items: center;
      gap: 4px;
    }
  `
})
export class FolderPickerComponent {
  private readonly api = inject(ApiService);

  readonly label       = input('Dossier');
  readonly placeholder = input('C:\\Music');
  readonly initialPath = input('');
  readonly folderChange = output<string>();

  readonly showBrowser = signal(false);
  readonly browsePage  = signal<FileBrowserPage | null>(null);
  readonly loading     = signal(false);
  readonly error       = signal<string | null>(null);

  // Lit initialPath() une seule fois à la construction. Si le parent change cette input
  // après création, pathInput ne se mettra pas à jour — utiliser linkedSignal si nécessaire.
  readonly pathInput = signal(this.initialPath());
  currentPage = 0;

  readonly dirs = computed(() =>
    (this.browsePage()?.entries ?? []).filter((e) => e.isDirectory)
  );

  readonly breadcrumbs = computed((): Breadcrumb[] => {
    const dir = this.browsePage()?.directory ?? '';
    const normalized = dir.replace(/\\/g, '/');
    const parts = normalized.split('/').filter(Boolean);
    return parts.map((seg, i) => {
      let path: string;
      if (/^[a-zA-Z]:$/.test(parts[0])) {
        path = i === 0 ? `${parts[0]}/` : `${parts[0]}/${parts.slice(1, i + 1).join('/')}`;
      } else {
        path = '/' + parts.slice(0, i + 1).join('/');
      }
      return { label: seg, path };
    });
  });

  open(path: string, page = 0): void {
    const p = (path || this.pathInput()).trim();
    if (!p) {
      this.error.set('Veuillez entrer un chemin.');
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.currentPage = page;
    this.api.browsePath(p, page).subscribe({
      next: (result) => {
        this.browsePage.set(result);
        this.pathInput.set(result.directory);
        this.showBrowser.set(true);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        const detail = err.error?.detail ?? err.error?.message ?? err.statusText;
        this.error.set(detail ?? `Erreur ${err.status}`);
        this.loading.set(false);
      }
    });
  }

  select(): void {
    const dir = this.browsePage()?.directory;
    if (!dir) return;
    this.pathInput.set(dir);
    this.closeBrowser();
    this.folderChange.emit(dir);
  }

  closeBrowser(): void {
    this.showBrowser.set(false);
  }

  prevPage(): void {
    if (this.currentPage > 0) this.open(this.browsePage()!.directory, this.currentPage - 1);
  }

  nextPage(): void {
    const p = this.browsePage();
    if (p && this.currentPage < p.totalPages - 1) this.open(p.directory, this.currentPage + 1);
  }
}
