import { Component, inject, signal, OnInit } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DatePipe } from '@angular/common';
import { StatsService } from '../../services/stats.service';
import { NotificationService } from '../../services/notification.service';
import { StatsReport } from '../../models/types';

@Component({
  selector: 'app-stats',
  standalone: true,
  imports: [MatCardModule, MatProgressBarModule, MatIconModule, MatProgressSpinnerModule, DatePipe],
  template: `
    <div class="stats-page">
      <h2>Statistiques de la collection</h2>

      @if (loading()) {
        <div class="center"><mat-spinner diameter="40" /></div>
      } @else if (stats()) {
        <div class="kpi-grid">
          <mat-card class="kpi-card">
            <mat-card-content>
              <mat-icon>assignment</mat-icon>
              <div class="kpi-value">{{ stats()!.totalPlansCreated }}</div>
              <div class="kpi-label">Plans créés</div>
            </mat-card-content>
          </mat-card>

          <mat-card class="kpi-card">
            <mat-card-content>
              <mat-icon>label</mat-icon>
              <div class="kpi-value">{{ stats()!.totalTagsApplied }}</div>
              <div class="kpi-label">Tags appliqués</div>
            </mat-card-content>
          </mat-card>

          <mat-card class="kpi-card">
            <mat-card-content>
              <mat-icon>audio_file</mat-icon>
              <div class="kpi-value">{{ stats()!.totalFilesEnriched }}</div>
              <div class="kpi-label">Fichiers enrichis</div>
            </mat-card-content>
          </mat-card>

          <mat-card class="kpi-card">
            <mat-card-content>
              <mat-icon>bar_chart</mat-icon>
              <div class="kpi-value">{{ tagTypeCount() }}</div>
              <div class="kpi-label">Types de tags</div>
            </mat-card-content>
          </mat-card>
        </div>

        @if (sortedTagTypes().length > 0) {
          <mat-card class="chart-card">
            <mat-card-header>
              <mat-card-title>Tags par type</mat-card-title>
            </mat-card-header>
            <mat-card-content>
              @for (entry of sortedTagTypes(); track entry.key) {
                <div class="tag-row">
                  <span class="tag-name">{{ entry.key }}</span>
                  <mat-progress-bar
                    mode="determinate"
                    [value]="(entry.value / maxTagCount()) * 100"
                    class="tag-bar" />
                  <span class="tag-count">{{ entry.value }}</span>
                </div>
              }
            </mat-card-content>
          </mat-card>
        }

        @if (stats()!.recentActivity.length > 0) {
          <mat-card>
            <mat-card-header>
              <mat-card-title>Activité récente</mat-card-title>
            </mat-card-header>
            <mat-card-content>
              <div class="activity-list">
                @for (entry of stats()!.recentActivity; track entry.filepath + entry.appliedAt) {
                  <div class="activity-item">
                    <mat-icon [class]="entry.success ? 'success' : 'failure'">
                      {{ entry.success ? 'check_circle' : 'error' }}
                    </mat-icon>
                    <span class="activity-file">{{ extractFilename(entry.filepath) }}</span>
                    <span class="activity-date">{{ entry.appliedAt | date:'short' }}</span>
                  </div>
                }
              </div>
            </mat-card-content>
          </mat-card>
        }
      }
    </div>
  `,
  styles: `
    .stats-page {
      padding: 24px;
      max-width: 900px;
      margin: 0 auto;
      display: flex;
      flex-direction: column;
      gap: 16px;
    }
    .center {
      display: flex;
      justify-content: center;
      padding: 48px;
    }
    .kpi-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
      gap: 16px;
    }
    .kpi-card mat-card-content {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 4px;
      padding: 20px 16px;
      text-align: center;
      mat-icon {
        color: var(--mat-sys-primary);
        font-size: 32px;
        width: 32px;
        height: 32px;
      }
    }
    .kpi-value {
      font-size: 2rem;
      font-weight: 600;
      color: var(--mat-sys-on-surface);
    }
    .kpi-label {
      font-size: 0.8rem;
      color: var(--mat-sys-on-surface-variant);
    }
    .chart-card mat-card-content {
      display: flex;
      flex-direction: column;
      gap: 10px;
      padding-top: 12px;
    }
    .tag-row {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .tag-name {
      min-width: 80px;
      font-size: 0.85rem;
      font-weight: 500;
    }
    .tag-bar {
      flex: 1;
    }
    .tag-count {
      min-width: 32px;
      text-align: right;
      font-size: 0.8rem;
      color: var(--mat-sys-on-surface-variant);
    }
    .activity-list {
      display: flex;
      flex-direction: column;
      gap: 8px;
      padding-top: 8px;
    }
    .activity-item {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 0.85rem;
      mat-icon {
        font-size: 18px;
        width: 18px;
        height: 18px;
      }
    }
    .activity-file {
      flex: 1;
      font-family: monospace;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .activity-date {
      color: var(--mat-sys-on-surface-variant);
      white-space: nowrap;
    }
    .success { color: #4caf50; }
    .failure { color: #f44336; }
  `,
})
export default class StatsComponent implements OnInit {
  private statsService = inject(StatsService);
  private notif = inject(NotificationService);

  readonly loading = signal(true);
  readonly stats = signal<StatsReport | null>(null);

  ngOnInit(): void {
    this.statsService.getStats().subscribe({
      next: data => {
        this.stats.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.notif.error('Impossible de charger les statistiques.');
      },
    });
  }

  protected tagTypeCount(): number {
    return Object.keys(this.stats()?.tagsAppliedByType ?? {}).length;
  }

  protected sortedTagTypes(): { key: string; value: number }[] {
    const map = this.stats()?.tagsAppliedByType ?? {};
    return Object.entries(map)
      .map(([key, value]) => ({ key, value }))
      .sort((a, b) => b.value - a.value);
  }

  protected maxTagCount(): number {
    const values = this.sortedTagTypes().map(e => e.value);
    return values.length > 0 ? Math.max(...values) : 1;
  }

  protected extractFilename(filepath: string): string {
    return filepath.split(/[/\\]/).pop() ?? filepath;
  }
}
