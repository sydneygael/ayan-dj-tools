import { Component, inject, signal, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog } from '@angular/material/dialog';
import { PlanService } from '../../services/plan.service';
import { TaggingPlan, BatchApplyResult, OperationStatus } from '../../models/types';
import { PlanSummaryComponent } from './plan-summary/plan-summary.component';
import { OperationCardComponent } from './operation-card/operation-card.component';
import { PlanProgressComponent } from './plan-progress/plan-progress.component';
import { ConfirmDialogComponent } from '../dialogs/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-plan-review',
  standalone: true,
  imports: [
    MatButtonModule, MatIconModule, MatProgressSpinnerModule,
    PlanSummaryComponent, OperationCardComponent, PlanProgressComponent,
  ],
  template: `
    @if (loading()) {
      <div class="center">
        <mat-spinner diameter="48" />
      </div>
    } @else if (error()) {
      <div class="center error">
        <mat-icon>error_outline</mat-icon>
        <p>{{ error() }}</p>
        <button mat-button (click)="loadPlan()">Reessayer</button>
      </div>
    } @else if (plan()) {
      <div class="plan-page">
        <app-plan-summary [plan]="plan()!" />

        <div class="actions">
          @if (plan()!.status === 'DRAFT') {
            <button mat-flat-button (click)="approveAll()">
              <mat-icon>done_all</mat-icon> Tout approuver
            </button>
          }
          @if (plan()!.status === 'APPROVED') {
            <button mat-flat-button (click)="executePlan()">
              <mat-icon>play_arrow</mat-icon> Executer
            </button>
          }
          <button mat-button color="warn" (click)="deletePlan()">
            <mat-icon>delete</mat-icon> Supprimer
          </button>
        </div>

        <app-plan-progress
          [result]="executionResult()"
          [total]="plan()!.operations.length"
          [executing]="executing()" />

        <div class="operations">
          @for (op of plan()!.operations; track op.filepath) {
            <app-operation-card
              [operation]="op"
              (approved)="approveOperation($event)"
              (rejected)="rejectOperation($event)" />
          }
        </div>
      </div>
    }
  `,
  styles: `
    .center {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 300px;
      gap: 12px;
    }
    .error mat-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
      color: #f44336;
    }
    .plan-page {
      padding: 24px;
      max-width: 900px;
      margin: 0 auto;
    }
    .actions {
      display: flex;
      gap: 8px;
      margin-bottom: 16px;
    }
    .operations {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
  `,
})
/**
 * Page de revue d'un plan de tagging.
 * Workflow : chargement du plan → revue des operations → approbation → execution → resultat.
 * Les statuts des operations sont modifiables localement (approuver/rejeter) avant l'approbation globale.
 */
export default class PlanReviewComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private planService = inject(PlanService);
  private dialog = inject(MatDialog);

  readonly plan = signal<TaggingPlan | null>(null);
  readonly loading = signal(false);
  readonly executing = signal(false);
  readonly error = signal<string | null>(null);
  readonly executionResult = signal<BatchApplyResult | null>(null);

  private planId = '';

  ngOnInit(): void {
    this.planId = this.route.snapshot.paramMap.get('id')!;
    this.loadPlan();
  }

  loadPlan(): void {
    this.loading.set(true);
    this.error.set(null);
    this.planService.get(this.planId).subscribe({
      next: plan => {
        this.plan.set(plan);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Impossible de charger le plan.');
        this.loading.set(false);
      },
    });
  }

  approveAll(): void {
    this.planService.approve(this.planId).subscribe({
      next: plan => this.plan.set(plan),
      error: () => this.error.set('Erreur lors de l\'approbation.'),
    });
  }

  executePlan(): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Executer le plan',
        message: `Appliquer les tags sur ${this.plan()!.operations.length} fichier(s) ? Cette action modifie les fichiers audio.`,
        confirmLabel: 'Executer',
        warn: true,
      },
    });
    ref.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.executing.set(true);
      this.planService.execute(this.planId).subscribe({
        next: result => {
          this.executionResult.set(result);
          this.executing.set(false);
          this.loadPlan();
        },
        error: () => {
          this.error.set('Erreur lors de l\'execution.');
          this.executing.set(false);
        },
      });
    });
  }

  deletePlan(): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Supprimer le plan',
        message: 'Supprimer definitivement ce plan ?',
        confirmLabel: 'Supprimer',
        warn: true,
      },
    });
    ref.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.planService.delete(this.planId).subscribe({
        next: () => this.router.navigate(['/']),
        error: () => this.error.set('Erreur lors de la suppression.'),
      });
    });
  }

  approveOperation(filepath: string): void {
    this.updateOperationStatus(filepath, OperationStatus.APPROVED);
  }

  rejectOperation(filepath: string): void {
    this.updateOperationStatus(filepath, OperationStatus.REJECTED);
  }

  /** Met a jour le statut d'une operation localement (cote client, sans appel backend). */
  private updateOperationStatus(filepath: string, status: OperationStatus): void {
    const current = this.plan();
    if (!current) return;
    const operations = current.operations.map(op =>
      op.filepath === filepath ? { ...op, status } : op,
    );
    this.plan.set({ ...current, operations });
  }
}
