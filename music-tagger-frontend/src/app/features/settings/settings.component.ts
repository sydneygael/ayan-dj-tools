import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { environment } from '../../../environments/environment';
import { ModeService } from '../../services/mode.service';
import { ThemeService } from '../../services/theme.service';
import { NotificationService } from '../../services/notification.service';
import { OperatingMode } from '../../models/types';

interface SettingsExport {
  apiUrl: string;
  defaultMode: OperatingMode;
  darkTheme: boolean;
  wsEnabled: boolean;
}

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [FormsModule, MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatSlideToggleModule, MatIconModule, MatButtonModule],
  template: `
    <div class="settings-page">
      <h2>Parametres</h2>

      <mat-card>
        <mat-card-header>
          <mat-icon mat-card-avatar>dns</mat-icon>
          <mat-card-title>Connexion</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>API URL</mat-label>
            <input matInput [value]="apiUrl" readonly />
          </mat-form-field>

          <mat-slide-toggle [(ngModel)]="wsEnabled" (ngModelChange)="saveWsEnabled($event)">
            WebSocket active
          </mat-slide-toggle>
        </mat-card-content>
      </mat-card>

      <mat-card>
        <mat-card-header>
          <mat-icon mat-card-avatar>tune</mat-icon>
          <mat-card-title>Preferences</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Mode par defaut</mat-label>
            <mat-select [value]="modeService.mode()" (selectionChange)="setDefaultMode($event.value)">
              <mat-option value="PLAN">Plan</mat-option>
              <mat-option value="MANUAL">Manuel</mat-option>
              <mat-option value="APPLY">Auto</mat-option>
            </mat-select>
          </mat-form-field>

          <mat-slide-toggle [checked]="themeService.isDark()" (change)="themeService.toggle()">
            Thème sombre
          </mat-slide-toggle>
        </mat-card-content>
      </mat-card>

      <mat-card>
        <mat-card-header>
          <mat-icon mat-card-avatar>import_export</mat-icon>
          <mat-card-title>Export / Import</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <div class="export-import-row">
            <button mat-stroked-button (click)="exportSettings()">
              <mat-icon>download</mat-icon>
              Exporter les parametres
            </button>
            <button mat-stroked-button (click)="importInput.click()">
              <mat-icon>upload</mat-icon>
              Importer les parametres
            </button>
            <input #importInput type="file" accept=".json" style="display:none" (change)="importSettings($event)" />
          </div>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: `
    .settings-page {
      padding: 24px;
      max-width: 600px;
      margin: 0 auto;
      display: flex;
      flex-direction: column;
      gap: 16px;
    }
    h2 {
      margin-bottom: 8px;
    }
    .full-width {
      width: 100%;
    }
    mat-card-content {
      display: flex;
      flex-direction: column;
      gap: 16px;
      padding-top: 16px;
    }
    .export-import-row {
      display: flex;
      gap: 12px;
      flex-wrap: wrap;
    }
  `,
})
/**
 * Page de parametres — connexion, preferences, theme, export/import JSON.
 */
export default class SettingsComponent {
  protected modeService = inject(ModeService);
  protected themeService = inject(ThemeService);
  private notif = inject(NotificationService);
  protected apiUrl = environment.apiUrl;
  protected wsEnabled = localStorage.getItem('wsEnabled') !== 'false';

  setDefaultMode(mode: OperatingMode): void {
    this.modeService.setMode(mode);
    localStorage.setItem('defaultMode', mode);
  }

  saveWsEnabled(enabled: boolean): void {
    localStorage.setItem('wsEnabled', String(enabled));
  }

  exportSettings(): void {
    const data: SettingsExport = {
      apiUrl: this.apiUrl,
      defaultMode: this.modeService.mode(),
      darkTheme: this.themeService.isDark(),
      wsEnabled: this.wsEnabled,
    };
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'ayan-settings.json';
    a.click();
    URL.revokeObjectURL(url);
    this.notif.success('Paramètres exportés.');
  }

  importSettings(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      try {
        const data = JSON.parse(reader.result as string) as Partial<SettingsExport>;
        if (!data || typeof data !== 'object') throw new Error('Format invalide');
        if (data.defaultMode && ['PLAN', 'MANUAL', 'APPLY'].includes(data.defaultMode)) {
          this.setDefaultMode(data.defaultMode);
        }
        if (typeof data.darkTheme === 'boolean') {
          if (data.darkTheme !== this.themeService.isDark()) {
            this.themeService.toggle();
          }
        }
        if (typeof data.wsEnabled === 'boolean') {
          this.wsEnabled = data.wsEnabled;
          this.saveWsEnabled(data.wsEnabled);
        }
        this.notif.success('Paramètres importés avec succès.');
      } catch {
        this.notif.error('Fichier de paramètres invalide.');
      }
    };
    reader.readAsText(file);
    (event.target as HTMLInputElement).value = '';
  }
}
