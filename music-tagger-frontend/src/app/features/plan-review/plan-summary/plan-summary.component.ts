import { Component, input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { DatePipe } from '@angular/common';
import { TaggingPlan, PlanStatus } from '../../../models/types';

@Component({
  selector: 'app-plan-summary',
  standalone: true,
  imports: [MatCardModule, MatChipsModule, MatIconModule, DatePipe],
  template: `
    <mat-card class="summary-card">
      <mat-card-header>
        <mat-icon mat-card-avatar>assignment</mat-icon>
        <mat-card-title>Plan {{ plan().planId.slice(0, 8) }}...</mat-card-title>
        <mat-card-subtitle>{{ plan().createdAt | date:'medium' }}</mat-card-subtitle>
      </mat-card-header>
      <mat-card-content>
        <div class="stats">
          <div class="stat">
            <span class="stat-value">{{ plan().totalFiles }}</span>
            <span class="stat-label">Fichiers</span>
          </div>
          <div class="stat">
            <span class="stat-value">{{ plan().filesWithMissingTags }}</span>
            <span class="stat-label">Tags manquants</span>
          </div>
          <div class="stat">
            <span class="stat-value">{{ plan().operations.length }}</span>
            <span class="stat-label">Operations</span>
          </div>
        </div>
        <mat-chip [class]="'status-' + plan().status.toLowerCase()">
          {{ statusLabel(plan().status) }}
        </mat-chip>
      </mat-card-content>
    </mat-card>
  `,
  styles: `
    .summary-card {
      margin-bottom: 16px;
    }
    .stats {
      display: flex;
      gap: 24px;
      margin: 16px 0;
    }
    .stat {
      display: flex;
      flex-direction: column;
      align-items: center;
    }
    .stat-value {
      font-size: 1.5rem;
      font-weight: 600;
      color: var(--mat-sys-primary);
    }
    .stat-label {
      font-size: 0.75rem;
      color: var(--mat-sys-on-surface-variant);
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }
    .status-draft { --mat-chip-label-text-color: #ff9800; }
    .status-approved { --mat-chip-label-text-color: #4caf50; }
    .status-executing { --mat-chip-label-text-color: #2196f3; }
    .status-completed { --mat-chip-label-text-color: #66bb6a; }
    .status-failed { --mat-chip-label-text-color: #f44336; }
  `,
})
/** Carte resumant un plan de tagging : ID, date, nombre de fichiers/operations, statut avec code couleur. */
export class PlanSummaryComponent {
  readonly plan = input.required<TaggingPlan>();

  statusLabel(status: PlanStatus): string {
    const labels: Record<PlanStatus, string> = {
      [PlanStatus.DRAFT]: 'Brouillon',
      [PlanStatus.APPROVED]: 'Approuve',
      [PlanStatus.EXECUTING]: 'En cours',
      [PlanStatus.COMPLETED]: 'Termine',
      [PlanStatus.FAILED]: 'Echoue',
    };
    return labels[status];
  }
}
