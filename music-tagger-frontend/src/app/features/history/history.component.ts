import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { DatePipe } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { PlanService } from '../../services/plan.service';
import { TaggingHistoryEntry } from '../../models/types';

@Component({
  selector: 'app-history',
  standalone: true,
  imports: [FormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule, MatTableModule, DatePipe],
  template: `
    <div class="history-page">
      <h2>Historique des modifications</h2>

      <div class="search-bar">
        <mat-form-field appearance="outline">
          <mat-label>Plan ID</mat-label>
          <input matInput [(ngModel)]="searchPlanId" placeholder="Entrer un ID de plan" />
        </mat-form-field>
        <button mat-flat-button (click)="search()" [disabled]="!searchPlanId">
          <mat-icon>search</mat-icon> Rechercher
        </button>
      </div>

      @if (loading()) {
        <p class="status-text">Chargement...</p>
      } @else if (error()) {
        <p class="status-text error">{{ error() }}</p>
      } @else if (entries().length > 0) {
        <table mat-table [dataSource]="entries()" class="history-table">
          <ng-container matColumnDef="status">
            <th mat-header-cell *matHeaderCellDef></th>
            <td mat-cell *matCellDef="let entry">
              <mat-icon [class]="entry.success ? 'success' : 'failure'">
                {{ entry.success ? 'check_circle' : 'error' }}
              </mat-icon>
            </td>
          </ng-container>

          <ng-container matColumnDef="filepath">
            <th mat-header-cell *matHeaderCellDef>Fichier</th>
            <td mat-cell *matCellDef="let entry" class="filepath-cell">{{ extractFilename(entry.filepath) }}</td>
          </ng-container>

          <ng-container matColumnDef="changes">
            <th mat-header-cell *matHeaderCellDef>Modifications</th>
            <td mat-cell *matCellDef="let entry">{{ entry.changes.length }} tag(s)</td>
          </ng-container>

          <ng-container matColumnDef="date">
            <th mat-header-cell *matHeaderCellDef>Date</th>
            <td mat-cell *matCellDef="let entry">{{ entry.appliedAt | date:'short' }}</td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;" (click)="toggleRow(row)"
              class="clickable-row"></tr>
        </table>

        @if (expandedEntry()) {
          <div class="detail-panel">
            <h4>Details : {{ extractFilename(expandedEntry()!.filepath) }}</h4>
            <div class="changes-list">
              @for (change of expandedEntry()!.changes; track change.tagName) {
                <div class="change-item">
                  <span class="change-tag">{{ change.tagName }}</span>
                  <span class="change-old">{{ change.oldValue || '—' }}</span>
                  <mat-icon>arrow_forward</mat-icon>
                  <span class="change-new">{{ change.newValue }}</span>
                </div>
              }
            </div>
          </div>
        }
      } @else if (searched()) {
        <p class="status-text">Aucun historique trouve pour ce plan.</p>
      }
    </div>
  `,
  styles: `
    .history-page {
      padding: 24px;
      max-width: 900px;
      margin: 0 auto;
    }
    h2 {
      margin-bottom: 16px;
    }
    .search-bar {
      display: flex;
      gap: 12px;
      align-items: flex-start;
      margin-bottom: 16px;
    }
    .search-bar mat-form-field {
      flex: 1;
    }
    .search-bar button {
      margin-top: 4px;
    }
    .status-text {
      text-align: center;
      color: var(--mat-sys-on-surface-variant);
      margin-top: 24px;
    }
    .error {
      color: #f44336;
    }
    .history-table {
      width: 100%;
    }
    .filepath-cell {
      font-family: monospace;
      font-size: 0.85rem;
    }
    .success { color: #4caf50; }
    .failure { color: #f44336; }
    .clickable-row { cursor: pointer; }
    .detail-panel {
      margin-top: 16px;
      padding: 16px;
      border: 1px solid var(--mat-sys-outline-variant);
      border-radius: 8px;
    }
    .changes-list {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    .change-item {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 0.85rem;
    }
    .change-tag {
      font-weight: 500;
      min-width: 80px;
    }
    .change-old {
      color: #f44336;
      text-decoration: line-through;
    }
    .change-new {
      color: #4caf50;
      font-weight: 500;
    }
    .change-item mat-icon {
      font-size: 16px;
      width: 16px;
      height: 16px;
      color: var(--mat-sys-on-surface-variant);
    }
  `,
})
/**
 * Page d'historique des modifications de tags.
 * Permet de rechercher par planId (pre-rempli si queryParam ?planId= present).
 * Affiche un tableau avec lignes cliquables pour voir le detail des changements.
 */
export default class HistoryComponent {
  private planService = inject(PlanService);
  private route = inject(ActivatedRoute);

  searchPlanId = '';
  readonly entries = signal<TaggingHistoryEntry[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly searched = signal(false);
  readonly expandedEntry = signal<TaggingHistoryEntry | null>(null);
  readonly displayedColumns = ['status', 'filepath', 'changes', 'date'];

  constructor() {
    // Pre-remplit et lance la recherche si un planId est passe en queryParam
    const planId = this.route.snapshot.queryParamMap.get('planId');
    if (planId) {
      this.searchPlanId = planId;
      this.search();
    }
  }

  search(): void {
    if (!this.searchPlanId) return;
    this.loading.set(true);
    this.error.set(null);
    this.expandedEntry.set(null);
    this.planService.getHistory(this.searchPlanId).subscribe({
      next: entries => {
        this.entries.set(entries);
        this.loading.set(false);
        this.searched.set(true);
      },
      error: () => {
        this.error.set('Erreur lors du chargement de l\'historique.');
        this.loading.set(false);
        this.searched.set(true);
      },
    });
  }

  toggleRow(entry: TaggingHistoryEntry): void {
    this.expandedEntry.set(this.expandedEntry() === entry ? null : entry);
  }

  extractFilename(filepath: string): string {
    return filepath.split(/[/\\]/).pop() || filepath;
  }
}
