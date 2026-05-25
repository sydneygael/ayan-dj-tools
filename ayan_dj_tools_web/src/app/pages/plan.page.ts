import { CommonModule, KeyValuePipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { firstValueFrom, map } from 'rxjs';
import { ApiService } from '../core/api.service';
import { PlanProgressResponse, TagOperation, TaggingPlan } from '../core/models';

@Component({
  standalone: true,
  selector: 'app-plan-page',
  imports: [CommonModule, KeyValuePipe],
  template: `
    <h1>Plan</h1>
    <p class="hint">Revision et execution du plan de tagging (HTTP only).</p>

    @if (loading()) {
      <p>Chargement...</p>
    } @else if (error()) {
      <p class="error">{{ error() }}</p>
    } @else if (plan()) {
      <section class="panel">
        <div class="row">
          <div><strong>ID:</strong> {{ plan()!.planId }}</div>
          <div><strong>Status:</strong> {{ plan()!.status }}</div>
          <div><strong>Mode:</strong> {{ plan()!.mode }}</div>
          <div><strong>Ops:</strong> {{ plan()!.operations.length }}</div>
        </div>
        <div class="row">
          <div><strong>Applied:</strong> {{ appliedCount() }}</div>
          <div><strong>Error:</strong> {{ errorCount() }}</div>
          <div><strong>Pending:</strong> {{ pendingCount() }}</div>
          <div><strong>HTTP Polling:</strong> {{ pollingActive() ? 'ON' : 'OFF' }}</div>
        </div>
        <div class="row">
          <button (click)="reload()">Rafraichir</button>
          <button (click)="approve()" [disabled]="busy()">Approuver</button>
          <button class="primary" (click)="execute()" [disabled]="busy()">Executer</button>
          <button class="primary" (click)="autoExecute()" [disabled]="busy()">Auto Execute</button>
        </div>
      </section>

      <section class="panel">
        <h2>Mode MANUAL</h2>
        <div class="row">
          <button (click)="loadCurrentOperation()">Charger l'operation courante</button>
          <button (click)="confirmCurrent(true)" [disabled]="!currentOperation()">Confirmer</button>
          <button (click)="confirmCurrent(false)" [disabled]="!currentOperation()">Rejeter</button>
        </div>
        @if (currentOperation()) {
          <div class="mono" style="margin-bottom: 8px">{{ currentOperation()!.filepath }}</div>
          <table>
            <thead><tr><th>Tag</th><th>Actuel</th><th>Suggéré</th></tr></thead>
            <tbody>
              @for (kv of (currentOperation()!.suggestedTags | keyvalue); track kv.key) {
                <tr>
                  <td class="mono">{{ kv.key }}</td>
                  <td>{{ currentOperation()!.currentTags[kv.key] || '—' }}</td>
                  <td>{{ kv.value }}</td>
                </tr>
              }
            </tbody>
          </table>
        }
      </section>

      <section class="panel">
        <h2>Operations</h2>
        <table>
          <thead>
            <tr>
              <th>Fichier</th>
              <th>Status</th>
              <th>Suggestions</th>
              <th>Message</th>
            </tr>
          </thead>
          <tbody>
            @for (op of plan()!.operations; track op.filepath) {
              <tr>
                <td class="mono">{{ op.filepath }}</td>
                <td>{{ op.status }}</td>
                <td>{{ countKeys(op.suggestedTags) }}</td>
                <td>{{ op.message || '' }}</td>
              </tr>
            }
          </tbody>
        </table>
      </section>
    }
  `,
  styles: `
    .row { margin-bottom: 8px; }
  `
})
export class PlanPageComponent {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly error = signal<string | null>(null);
  readonly plan = signal<TaggingPlan | null>(null);
  readonly progress = signal<PlanProgressResponse | null>(null);
  readonly currentOperation = signal<TagOperation | null>(null);
  readonly planId = signal('');
  readonly pollingActive = signal(false);

  readonly appliedCount = computed(() => this.progress()?.appliedCount ?? 0);
  readonly errorCount = computed(() => this.progress()?.errorCount ?? 0);
  readonly pendingCount = computed(
    () => (this.progress()?.pendingCount ?? 0) + (this.progress()?.approvedCount ?? 0)
  );

  private pollingTimer: number | null = null;

  constructor() {
    this.route.paramMap
      .pipe(
        map((params) => params.get('id') ?? ''),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((id) => {
        if (!id) {
          this.error.set('Plan ID manquant');
          this.loading.set(false);
          return;
        }
        this.planId.set(id);
        void this.loadPlan(id);
      });

    this.destroyRef.onDestroy(() => this.stopPolling());
  }

  async reload(): Promise<void> {
    await this.loadPlan(this.planId());
  }

  async approve(): Promise<void> {
    await this.wrapBusy(async () => {
      await firstValueFrom(this.api.approvePlan(this.planId()));
      await this.loadPlan(this.planId());
    });
  }

  async execute(): Promise<void> {
    await this.wrapBusy(async () => {
      await firstValueFrom(this.api.executePlan(this.planId()));
      await this.loadPlan(this.planId());
    });
  }

  async autoExecute(): Promise<void> {
    await this.wrapBusy(async () => {
      await firstValueFrom(this.api.autoExecutePlan(this.planId()));
      await this.loadPlan(this.planId());
      this.startPolling();
    });
  }

  async loadCurrentOperation(): Promise<void> {
    this.error.set(null);
    try {
      const op = await firstValueFrom(this.api.getCurrentOperation(this.planId()));
      this.currentOperation.set(op);
    } catch (error: unknown) {
      this.error.set(this.errorToMessage(error));
    }
  }

  async confirmCurrent(approved: boolean): Promise<void> {
    const operation = this.currentOperation();
    if (!operation) {
      return;
    }
    const index = this.plan()?.operations.findIndex((op) => op.filepath === operation.filepath) ?? -1;
    if (index < 0) {
      return;
    }
    await this.wrapBusy(async () => {
      await firstValueFrom(this.api.confirmOperation(this.planId(), index, approved));
      await this.loadPlan(this.planId());
      await this.loadCurrentOperation();
    });
  }

  countKeys(map: Record<string, string>): number {
    return Object.keys(map ?? {}).length;
  }

  private async loadPlan(planId: string): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const plan = await firstValueFrom(this.api.getPlan(planId));
      this.plan.set(plan);
      this.progress.set(await firstValueFrom(this.api.getPlanProgress(planId)));
      if (plan.status === 'APPLYING') {
        this.startPolling();
      } else {
        this.stopPolling();
      }
    } catch (error: unknown) {
      this.error.set(this.errorToMessage(error));
      this.stopPolling();
    } finally {
      this.loading.set(false);
    }
  }

  private startPolling(): void {
    if (this.pollingTimer !== null) {
      this.pollingActive.set(true);
      return;
    }

    this.pollingActive.set(true);
    this.pollingTimer = window.setInterval(async () => {
      try {
        const progress = await firstValueFrom(this.api.getPlanProgress(this.planId()));
        this.progress.set(progress);
        if (progress.status !== 'APPLYING') {
          const plan = await firstValueFrom(this.api.getPlan(this.planId()));
          this.plan.set(plan);
          this.stopPolling();
        }
      } catch {
        this.stopPolling();
      }
    }, 2000);
  }

  private stopPolling(): void {
    if (this.pollingTimer !== null) {
      window.clearInterval(this.pollingTimer);
      this.pollingTimer = null;
    }
    this.pollingActive.set(false);
  }

  private async wrapBusy(action: () => Promise<void>): Promise<void> {
    this.busy.set(true);
    this.error.set(null);
    try {
      await action();
    } catch (error: unknown) {
      this.error.set(this.errorToMessage(error));
    } finally {
      this.busy.set(false);
    }
  }

  private errorToMessage(error: unknown): string {
    if (typeof error === 'string') {
      return error;
    }
    if (error instanceof Error) {
      return error.message;
    }
    return 'Erreur lors du chargement du plan';
  }
}
