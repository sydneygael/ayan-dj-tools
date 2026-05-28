import { CommonModule, DatePipe, JsonPipe } from '@angular/common';
import { Component, inject, resource, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiService } from '../core/api.service';
import { TaggingHistoryEntry } from '../core/models';

@Component({
  standalone: true,
  selector: 'app-history-page',
  imports: [CommonModule, DatePipe, JsonPipe],
  template: `
    <h1>Historique</h1>
    <p class="hint">Recherche par planId pour voir les changements de tags appliqués.</p>

    <section class="panel">
      <div class="row">
        <input
          [value]="planIdInput()"
          (input)="planIdInput.set($any($event.target).value)"
          placeholder="planId..."
        />
        <button (click)="search()">Rechercher</button>
      </div>
      @if (historyResource.error()) {
        <p class="error">Erreur lors de la récupération de l'historique</p>
      }
    </section>

    <section class="panel">
      <h2>Résultats ({{ (historyResource.value() ?? []).length }})</h2>
      @if (historyResource.isLoading()) {
        <p class="hint">Chargement...</p>
      } @else if ((historyResource.value() ?? []).length === 0) {
        <p class="hint">Aucune entrée.</p>
      } @else {
        @for (entry of historyResource.value()!; track $index) {
          <div class="entry">
            <div class="row">
              <strong>{{ entry.filepath }}</strong>
              <span>{{ entry.status }}</span>
              <span>{{ entry.appliedAt | date: 'short' }}</span>
            </div>
            <div class="diff-grid">
              <div>
                <h3>Avant</h3>
                <pre>{{ entry.oldTags | json }}</pre>
              </div>
              <div>
                <h3>Après</h3>
                <pre>{{ entry.newTags | json }}</pre>
              </div>
            </div>
            @if (entry.errorMessage) {
              <p class="error">{{ entry.errorMessage }}</p>
            }
          </div>
        }
      }
    </section>
  `,
  styles: `
    input { flex: 1; min-width: 300px; }
    .entry { border: 1px solid var(--border); border-radius: 8px; padding: 10px; margin-bottom: 10px; }
    .diff-grid { display: grid; gap: 8px; grid-template-columns: 1fr 1fr; }
    @media (max-width: 900px) { .diff-grid { grid-template-columns: 1fr; } }
  `
})
export class HistoryPageComponent {
  private readonly api = inject(ApiService);

  readonly planIdInput = signal(''); // éditable librement sans déclencher de requête
  // searchTrigger séparé : seul le bouton "Rechercher" commit la valeur et lance le fetch,
  // évitant un appel API à chaque frappe clavier.
  readonly searchTrigger = signal('');

  readonly historyResource = resource({
    params: () => this.searchTrigger(), // ne réagit qu'au commit, pas à la saisie
    loader: ({ params: planId }): Promise<TaggingHistoryEntry[]> => {
      if (!planId.trim()) return Promise.resolve([]);
      return firstValueFrom(this.api.getPlanHistory(planId.trim()));
    },
  });

  search(): void {
    const id = this.planIdInput().trim();
    if (!id) return;
    // Même ID : resource() ne recharge pas sur un params identique, forcer via reload().
    if (this.searchTrigger() === id) {
      this.historyResource.reload();
    } else {
      this.searchTrigger.set(id);
    }
  }
}
