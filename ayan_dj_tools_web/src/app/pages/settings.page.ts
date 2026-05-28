import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { OperatingMode } from '../core/models';
import { PreferencesStore } from '../core/preferences.store';
import { FolderPickerComponent } from '../shared/folder-picker.component';

interface SettingsModel {
  apiBaseUrl: string;
  defaultMode: string;
  theme: string;
  language: string;
  defaultMusicDir: string;
  jsonBuffer: string;
}

@Component({
  standalone: true,
  selector: 'app-settings-page',
  imports: [
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    FolderPickerComponent,
  ],
  template: `
    <h1>Paramètres</h1>

    <mat-card class="settings-card">
      <mat-card-header>
        <mat-card-title>Configuration</mat-card-title>
      </mat-card-header>

      <mat-card-content>
        <p class="section-label">Dossier de musique par défaut</p>
        <app-folder-picker
          label="Dossier de musique"
          placeholder="C:\\Music ou /home/user/music"
          [initialPath]="model().defaultMusicDir"
          (folderChange)="onFolderChange($event)"
        />

        <mat-form-field class="full-width" style="margin-top: 16px">
          <mat-label>URL API backend</mat-label>
          <input
            matInput
            [value]="model().apiBaseUrl"
            (input)="model.update(m => ({...m, apiBaseUrl: $any($event.target).value}))"
          />
          <mat-icon matSuffix fontSet="material-symbols-rounded">api</mat-icon>
        </mat-form-field>

        <mat-form-field class="full-width">
          <mat-label>Mode par défaut</mat-label>
          <mat-select
            [value]="model().defaultMode"
            (selectionChange)="model.update(m => ({...m, defaultMode: $event.value}))">
            <mat-option value="PLAN">PLAN</mat-option>
            <mat-option value="MANUAL">MANUAL</mat-option>
            <mat-option value="APPLY">APPLY</mat-option>
          </mat-select>
        </mat-form-field>

        <mat-form-field class="full-width">
          <mat-label>Thème</mat-label>
          <mat-select
            [value]="model().theme"
            (selectionChange)="model.update(m => ({...m, theme: $event.value}))">
            <mat-option value="dark">Sombre</mat-option>
            <mat-option value="light">Clair</mat-option>
          </mat-select>
        </mat-form-field>

        <mat-form-field class="full-width">
          <mat-label>Langue</mat-label>
          <mat-select
            [value]="model().language"
            (selectionChange)="model.update(m => ({...m, language: $event.value}))">
            <mat-option value="fr">Français</mat-option>
            <mat-option value="en">English</mat-option>
          </mat-select>
        </mat-form-field>
      </mat-card-content>

      <mat-card-actions>
        <button mat-flat-button (click)="save()">
          <mat-icon fontSet="material-symbols-rounded">save</mat-icon>
          Sauvegarder
        </button>
        @if (saved()) {
          <span class="ok">
            <mat-icon fontSet="material-symbols-rounded">check_circle</mat-icon>
            Sauvegardé
          </span>
        }
      </mat-card-actions>
    </mat-card>

    <mat-card class="settings-card">
      <mat-card-header>
        <mat-card-title>Export / Import JSON</mat-card-title>
      </mat-card-header>

      <mat-card-content>
        <mat-form-field class="full-width">
          <mat-label>JSON des préférences</mat-label>
          <textarea
            matInput
            rows="7"
            [value]="model().jsonBuffer"
            (input)="model.update(m => ({...m, jsonBuffer: $any($event.target).value}))"
          ></textarea>
        </mat-form-field>
      </mat-card-content>

      <mat-card-actions>
        <button mat-stroked-button (click)="exportPrefs()">
          <mat-icon fontSet="material-symbols-rounded">download</mat-icon>
          Exporter
        </button>
        <button mat-stroked-button (click)="importPrefs()">
          <mat-icon fontSet="material-symbols-rounded">upload</mat-icon>
          Importer
        </button>
        @if (error()) {
          <p class="error">{{ error() }}</p>
        }
      </mat-card-actions>
    </mat-card>
  `,
  styles: `
    .settings-card {
      max-width: 600px;
      margin-bottom: 16px;
    }

    .full-width {
      width: 100%;
      margin-bottom: 8px;
    }

    .section-label {
      font-size: .8rem;
      color: var(--muted);
      text-transform: uppercase;
      letter-spacing: .05em;
      margin: 0 0 6px;
    }

    .ok {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      color: var(--success);
      font-size: .9rem;

      mat-icon { font-size: 18px; width: 18px; height: 18px; }
    }

    mat-card-actions {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;
    }
  `
})
export class SettingsPageComponent {
  private readonly prefs = inject(PreferencesStore);

  readonly saved = signal(false);
  readonly error = signal<string | null>(null);

  readonly model = signal<SettingsModel>({
    apiBaseUrl: this.prefs.apiBaseUrl(),
    defaultMode: this.prefs.defaultMode(),
    theme: this.prefs.theme(),
    language: this.prefs.language(),
    defaultMusicDir: this.prefs.defaultMusicDir(),
    jsonBuffer: '',
  });

  onFolderChange(dir: string): void {
    this.model.update((m) => ({ ...m, defaultMusicDir: dir }));
  }

  save(): void {
    const m = this.model();
    this.prefs.setApiBaseUrl(m.apiBaseUrl);
    // Cast nécessaire : MatSelectChange.value est typé `any` dans le template, donc le model
    // reçoit des strings. Les mat-options ne produisent que des valeurs valides de l'enum.
    this.prefs.setDefaultMode(m.defaultMode as OperatingMode);
    this.prefs.setTheme(m.theme as 'light' | 'dark');
    this.prefs.setLanguage(m.language as 'fr' | 'en');
    this.prefs.setDefaultMusicDir(m.defaultMusicDir);
    this.saved.set(true);
    setTimeout(() => this.saved.set(false), 1500);
  }

  exportPrefs(): void {
    this.error.set(null);
    this.model.update((m) => ({ ...m, jsonBuffer: this.prefs.exportJson() }));
  }

  importPrefs(): void {
    this.error.set(null);
    try {
      this.prefs.importJson(this.model().jsonBuffer);
      this.model.update((m) => ({
        ...m,
        apiBaseUrl: this.prefs.apiBaseUrl(),
        defaultMode: this.prefs.defaultMode(),
        theme: this.prefs.theme(),
        language: this.prefs.language(),
        defaultMusicDir: this.prefs.defaultMusicDir(),
      }));
    } catch {
      this.error.set('JSON invalide');
    }
  }
}
