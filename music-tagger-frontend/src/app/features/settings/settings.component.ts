import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatIconModule } from '@angular/material/icon';
import { environment } from '../../../environments/environment';
import { ModeService } from '../../services/mode.service';
import { OperatingMode } from '../../models/types';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [FormsModule, MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatSlideToggleModule, MatIconModule],
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
  `,
})
/**
 * Page de parametres de l'application.
 * Permet de configurer le mode par defaut (persiste dans localStorage) et d'activer/desactiver le WebSocket.
 */
export default class SettingsComponent {
  protected modeService = inject(ModeService);
  protected apiUrl = environment.apiUrl;
  protected wsEnabled = localStorage.getItem('wsEnabled') !== 'false';

  /** Change le mode et le persiste dans localStorage pour les prochaines sessions. */
  setDefaultMode(mode: OperatingMode): void {
    this.modeService.setMode(mode);
    localStorage.setItem('defaultMode', mode);
  }

  saveWsEnabled(enabled: boolean): void {
    localStorage.setItem('wsEnabled', String(enabled));
  }
}
