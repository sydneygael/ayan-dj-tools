import { Component, inject, resource, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { firstValueFrom } from 'rxjs';
import { ApiKeysView, ApiKeysSaveRequest, OperatingMode } from '../core/models';
import { ApiService } from '../core/api.service';
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

    <mat-card class="settings-card">
      <mat-card-header>
        <mat-card-title>Clés API</mat-card-title>
      </mat-card-header>

      <mat-card-content>
        @if (apiKeysResource.isLoading()) {
          <p class="hint">Chargement...</p>
        } @else if (apiKeysResource.hasValue()) {
          @for (field of apiKeyFields; track field.key) {
            <div class="api-key-row">
              <div class="api-key-label">
                <span class="api-key-name">{{ field.label }}</span>
                @if (apiKeysResource.value()![field.key].configured) {
                  <span class="status ok">
                    <mat-icon fontSet="material-symbols-rounded">check_circle</mat-icon>
                    {{ apiKeysResource.value()![field.key].masked }}
                  </span>
                } @else {
                  <span class="status missing">
                    <mat-icon fontSet="material-symbols-rounded">cancel</mat-icon>
                    Non configurée
                  </span>
                }
              </div>
              <mat-form-field class="full-width">
                <mat-label>Nouvelle valeur</mat-label>
                <input
                  matInput
                  type="password"
                  [value]="getApiKeyValue(field.key)"
                  (input)="setApiKey(field.key, $any($event.target).value)"
                  autocomplete="new-password"
                />
                <mat-icon matSuffix fontSet="material-symbols-rounded">key</mat-icon>
              </mat-form-field>
            </div>
          }
        }
      </mat-card-content>

      <mat-card-actions>
        <button mat-flat-button (click)="saveApiKeys()" [disabled]="apiKeysSaving()">
          <mat-icon fontSet="material-symbols-rounded">save</mat-icon>
          Sauvegarder les clés
        </button>
        @if (apiKeysSaved()) {
          <span class="ok">
            <mat-icon fontSet="material-symbols-rounded">check_circle</mat-icon>
            Sauvegardé
          </span>
        }
        @if (apiKeysError()) {
          <p class="error">{{ apiKeysError() }}</p>
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

    .api-key-row { margin-bottom: 12px; }
    .api-key-label { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
    .api-key-name { font-weight: 500; font-size: .9rem; }
    .status { display: inline-flex; align-items: center; gap: 3px; font-size: .8rem;
      mat-icon { font-size: 14px; width: 14px; height: 14px; } }
    .status.ok { color: var(--success, #4caf50); }
    .status.missing { color: var(--muted, #888); }
  `
})
export class SettingsPageComponent {
  private readonly prefs = inject(PreferencesStore);
  private readonly api = inject(ApiService);

  readonly saved = signal(false);
  readonly error = signal<string | null>(null);

  readonly apiKeysResource = resource({ loader: () => firstValueFrom(this.api.getApiKeys()) });
  readonly apiKeysModel = signal<Record<string, string>>({});
  readonly apiKeysSaving = signal(false);
  readonly apiKeysSaved = signal(false);
  readonly apiKeysError = signal<string | null>(null);

  readonly apiKeyFields: { key: keyof ApiKeysView; label: string }[] = [
    { key: 'soundchartsAppId',    label: 'Soundcharts App ID' },
    { key: 'soundchartsApiKey',   label: 'Soundcharts API Key' },
    { key: 'spotifyClientId',     label: 'Spotify Client ID' },
    { key: 'spotifyClientSecret', label: 'Spotify Client Secret' },
    { key: 'tavilyApiKey',        label: 'Tavily API Key' },
  ];

  getApiKeyValue(key: string): string {
    return this.apiKeysModel()[key] ?? '';
  }

  setApiKey(key: string, value: string): void {
    this.apiKeysModel.update(m => ({ ...m, [key]: value }));
  }

  saveApiKeys(): void {
    const m = this.apiKeysModel();
    const hasValues = Object.values(m).some(v => v && v.trim());
    if (!hasValues) return;
    this.apiKeysSaving.set(true);
    this.apiKeysError.set(null);
    firstValueFrom(this.api.saveApiKeys(m as ApiKeysSaveRequest))
      .then(() => {
        this.apiKeysSaved.set(true);
        this.apiKeysModel.set({});
        this.apiKeysResource.reload();
        setTimeout(() => this.apiKeysSaved.set(false), 2000);
      })
      .catch(() => this.apiKeysError.set('Erreur lors de la sauvegarde'))
      .finally(() => this.apiKeysSaving.set(false));
  }

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
