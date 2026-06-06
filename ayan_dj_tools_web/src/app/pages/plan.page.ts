import { CommonModule, KeyValuePipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, resource, signal } from '@angular/core';
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
    <p class="hint">Revision et execution du plan de tagging.</p>

    @if (planResource.isLoading()) {
      <p>Chargement...</p>
    } @else if (error()) {
      <p class="error">{{ error() }}</p>
    } @else if (planResource.error()) {
      <p class="error">Erreur lors du chargement du plan</p>
    } @else if (planResource.value()) {
      <section class="panel">
        <div class="row">
          <div><strong>ID:</strong> {{ planResource.value()!.planId }}</div>
          <div><strong>Status:</strong> {{ planResource.value()!.status }}</div>
          <div><strong>Mode:</strong> {{ planResource.value()!.mode }}</div>
          <div><strong>Ops:</strong> {{ planResource.value()!.operations.length }}</div>
        </div>
        <div class="row">
          <div><strong>Applied:</strong> {{ appliedCount() }}</div>
          <div><strong>Error:</strong> {{ errorCount() }}</div>
          <div><strong>Pending:</strong> {{ pendingCount() }}</div>
          <div><strong>HTTP Polling:</strong> {{ pollingActive() ? 'ON' : 'OFF' }}</div>
        </div>
        <div class="row">
          <button (click)="planResource.reload()">Rafraichir</button>
          <button (click)="approve()" [disabled]="busy()">Approuver</button>
          <button class="primary" (click)="execute()" [disabled]="busy()">Executer</button>
          <span class="danger-separator" aria-hidden="true">|</span>
          <button class="danger"
                  (click)="confirmAutoExecute()"
                  [disabled]="busy()"
                  title="Approuve et écrit tous les tags immédiatement sans relecture — irréversible sans rollback manuel">
            ⚠ Auto Execute
          </button>
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
            @for (op of planResource.value()!.operations; track op.filepath) {
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

    .danger-separator {
      color: var(--border, #444);
      margin: 0 4px;
      user-select: none;
    }

    button.danger {
      background: transparent;
      color: #e57373;
      border: 1px solid #e57373;
      border-radius: 4px;
      padding: 4px 12px;
      cursor: pointer;
      font-size: inherit;
      transition: background .15s, color .15s;
    }

    button.danger:hover:not(:disabled) {
      background: #e57373;
      color: #fff;
    }

    button.danger:disabled {
      opacity: .45;
      cursor: default;
    }
  `
})
export class PlanPageComponent {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  readonly busy = signal(false);
  readonly error = signal<string | null>(null);
  // Signal simple (pas resource) : mis à jour par le polling indépendamment des rechargements du plan.
  // Un resource() le réinitialiserait à undefined à chaque reload, cassant l'affichage en cours d'exécution.
  readonly progress = signal<PlanProgressResponse | null>(null);
  readonly currentOperation = signal<TagOperation | null>(null);
  readonly planId = signal('');
  readonly pollingActive = signal(false);

  // Le changement de planId() déclenche automatiquement le rechargement via les params réactifs.
  readonly planResource = resource({
    params: () => this.planId(),
    loader: ({ params: id }): Promise<TaggingPlan | null> => {
      if (!id) return Promise.resolve(null);
      return firstValueFrom(this.api.getPlan(id));
    },
  });

  readonly appliedCount = computed(() => this.progress()?.appliedCount ?? 0);
  readonly errorCount = computed(() => this.progress()?.errorCount ?? 0);
  readonly pendingCount = computed(
    () => (this.progress()?.pendingCount ?? 0) + (this.progress()?.approvedCount ?? 0)
  );

  private pollingTimer: number | null = null;

  constructor() {
    // La subscription route reste RxJS : ActivatedRoute n'expose pas d'API signal native.
    this.route.paramMap
      .pipe(
        map((params) => params.get('id') ?? ''),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((id) => {
        if (!id) {
          this.error.set('Plan ID manquant');
          return;
        }
        this.planId.set(id); // déclenche planResource via ses params réactifs
        void this.refreshProgress(id);
      });

    this.destroyRef.onDestroy(() => this.stopPolling());
  }

  async approve(): Promise<void> {
    await this.wrapBusy(async () => {
      await firstValueFrom(this.api.approvePlan(this.planId()));
      this.planResource.reload();
    });
  }

  async execute(): Promise<void> {
    await this.wrapBusy(async () => {
      await firstValueFrom(this.api.executePlan(this.planId()));
      this.planResource.reload();
      await this.refreshProgress(this.planId());
    });
  }

  async confirmAutoExecute(): Promise<void> {
    const count = this.planResource.value()?.operations.length ?? 0;
    const ok = window.confirm(
      `Auto Execute va approuver et écrire immédiatement les tags de ${count} fichier(s) sans étape de relecture.\n\nContinuer ?`
    );
    if (ok) await this.autoExecute();
  }

  async autoExecute(): Promise<void> {
    await this.wrapBusy(async () => {
      await firstValueFrom(this.api.autoExecutePlan(this.planId()));
      this.planResource.reload();
      await this.refreshProgress(this.planId());
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
    if (!operation) return;
    const plan = this.planResource.value();
    const index = plan?.operations.findIndex((op) => op.filepath === operation.filepath) ?? -1;
    if (index < 0) return;
    await this.wrapBusy(async () => {
      await firstValueFrom(this.api.confirmOperation(this.planId(), index, approved));
      this.planResource.reload();
      await this.loadCurrentOperation();
    });
  }

  countKeys(map: Record<string, string>): number {
    return Object.keys(map ?? {}).length;
  }

  private async refreshProgress(planId: string): Promise<void> {
    try {
      const prog = await firstValueFrom(this.api.getPlanProgress(planId));
      this.progress.set(prog);
      if (prog.status === 'APPLYING') {
        this.startPolling();
      } else {
        this.stopPolling();
      }
    } catch {
      this.stopPolling();
    }
  }

  // TODO : remplacer par STOMP /topic/plan/{id}/progress pour éviter le polling HTTP.
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
          this.planResource.reload();
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
    if (typeof error === 'string') return error;
    if (error instanceof Error) return error.message;
    return 'Erreur lors du chargement du plan';
  }
}
