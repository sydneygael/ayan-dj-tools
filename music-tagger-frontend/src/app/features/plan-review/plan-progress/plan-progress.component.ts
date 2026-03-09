import { Component, input, computed } from '@angular/core';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatIconModule } from '@angular/material/icon';
import { BatchApplyResult } from '../../../models/types';

@Component({
  selector: 'app-plan-progress',
  standalone: true,
  imports: [MatProgressBarModule, MatIconModule],
  template: `
    @if (executing()) {
      <div class="progress-section">
        <mat-progress-bar mode="indeterminate" />
        <p class="status-text">Execution en cours...</p>
      </div>
    }
    @if (result()) {
      <div class="progress-section">
        <mat-progress-bar mode="determinate" [value]="percentage()" />
        <div class="counters">
          <span class="success">
            <mat-icon>check_circle</mat-icon>
            {{ result()!.successCount }} reussi{{ result()!.successCount > 1 ? 's' : '' }}
          </span>
          @if (result()!.failureCount > 0) {
            <span class="failure">
              <mat-icon>error</mat-icon>
              {{ result()!.failureCount }} echoue{{ result()!.failureCount > 1 ? 's' : '' }}
            </span>
          }
          <span class="total">/ {{ total() }} total</span>
        </div>
      </div>
    }
  `,
  styles: `
    .progress-section {
      margin: 16px 0;
    }
    .status-text {
      text-align: center;
      color: var(--mat-sys-on-surface-variant);
      margin-top: 8px;
    }
    .counters {
      display: flex;
      align-items: center;
      gap: 16px;
      margin-top: 8px;
      font-size: 0.9rem;
    }
    .counters span {
      display: flex;
      align-items: center;
      gap: 4px;
    }
    .success {
      color: #4caf50;
    }
    .failure {
      color: #f44336;
    }
    .total {
      color: var(--mat-sys-on-surface-variant);
    }
    mat-icon {
      font-size: 18px;
      width: 18px;
      height: 18px;
    }
  `,
})
/**
 * Barre de progression de l'execution d'un plan.
 * Mode indeterminate pendant l'execution, puis determinate avec compteurs succes/echec apres le resultat.
 */
export class PlanProgressComponent {
  readonly result = input<BatchApplyResult | null>(null);
  readonly total = input<number>(0);
  readonly executing = input<boolean>(false);

  readonly percentage = computed(() => {
    const r = this.result();
    const t = this.total();
    if (!r || t === 0) return 0;
    return Math.round(((r.successCount + r.failureCount) / t) * 100);
  });
}
