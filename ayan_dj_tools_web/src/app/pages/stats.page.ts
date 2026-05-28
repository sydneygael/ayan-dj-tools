import { CommonModule, DecimalPipe, KeyValuePipe } from '@angular/common';
import { Component, inject, resource, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiService } from '../core/api.service';

@Component({
  standalone: true,
  selector: 'app-stats-page',
  imports: [CommonModule, KeyValuePipe, DecimalPipe],
  template: `
    <h1>Statistiques</h1>
    <div class="row" style="margin-bottom: 12px">
      <button (click)="reloadAll()">Rafraîchir</button>
      <select [value]="period()" (change)="period.set($any($event.target).value)">
        <option value="week">Semaine</option>
        <option value="month">Mois</option>
        <option value="all">Tout</option>
      </select>
      <button (click)="activityResource.reload()">Recharger activité</button>
    </div>

    @if (statsResource.error() || collectionResource.error() || enrichmentResource.error() || activityResource.error()) {
      <p class="error">Erreur lors du chargement des statistiques</p>
    }

    @if (statsResource.isLoading()) {
      <p class="hint">Chargement...</p>
    } @else if (statsResource.hasValue()) {
      <section class="panel">
        <h2>Global</h2>
        <div class="kv">
          <span>Plans créés</span><span>{{ statsResource.value()!.totalPlansCreated }}</span>
          <span>Tags appliqués</span><span>{{ statsResource.value()!.totalTagsApplied }}</span>
          <span>Fichiers enrichis</span><span>{{ statsResource.value()!.totalFilesEnriched }}</span>
        </div>
        @if ((statsResource.value()!.tagsAppliedByType | keyvalue).length > 0) {
          <h3>Tags par type</h3>
          <div class="kv">
            @for (kv of (statsResource.value()!.tagsAppliedByType | keyvalue); track kv.key) {
              <span>{{ kv.key }}</span><span>{{ kv.value }}</span>
            }
          </div>
        }
      </section>
    }

    @if (collectionResource.isLoading()) {
      <p class="hint">Chargement collection...</p>
    } @else if (collectionResource.hasValue()) {
      <section class="panel">
        <h2>Collection</h2>
        <div class="kv">
          <span>Tracks scannées</span><span>{{ collectionResource.value()!.totalTracksScanned }}</span>
          <span>Tracks enrichies</span><span>{{ collectionResource.value()!.totalTracksEnriched }}</span>
          <span>Tags complets</span><span>{{ collectionResource.value()!.totalWithCompleteTags }}</span>
        </div>
        @if ((collectionResource.value()!.genreDistribution | keyvalue).length > 0) {
          <h3>Genres</h3>
          <div class="kv">
            @for (kv of (collectionResource.value()!.genreDistribution | keyvalue); track kv.key) {
              <span>{{ kv.key }}</span><span>{{ kv.value }}</span>
            }
          </div>
        }
      </section>
    }

    @if (enrichmentResource.isLoading()) {
      <p class="hint">Chargement enrichissement...</p>
    } @else if (enrichmentResource.hasValue()) {
      <section class="panel">
        <h2>Enrichissement Spotify</h2>
        <div class="kv">
          <span>Taux de correspondance</span><span>{{ enrichmentResource.value()!.spotifyMatchRate | number:'1.0-1' }}%</span>
          <span>Taux d'erreur</span><span>{{ enrichmentResource.value()!.errorRate | number:'1.0-1' }}%</span>
        </div>
        @if ((enrichmentResource.value()!.mostEnrichedTagTypes | keyvalue).length > 0) {
          <h3>Tags les plus enrichis</h3>
          <div class="kv">
            @for (kv of (enrichmentResource.value()!.mostEnrichedTagTypes | keyvalue); track kv.key) {
              <span>{{ kv.key }}</span><span>{{ kv.value }}</span>
            }
          </div>
        }
      </section>
    }

    @if (activityResource.isLoading()) {
      <p class="hint">Chargement activité...</p>
    } @else if (activityResource.hasValue()) {
      <section class="panel">
        <h2>Activité — {{ period() }}</h2>
        @if ((activityResource.value()!.modeUsage | keyvalue).length > 0) {
          <h3>Utilisation par mode</h3>
          <div class="kv">
            @for (kv of (activityResource.value()!.modeUsage | keyvalue); track kv.key) {
              <span>{{ kv.key }}</span><span>{{ kv.value }}</span>
            }
          </div>
        }
        @if ((activityResource.value()!.plansPerPeriod | keyvalue).length > 0) {
          <h3>Plans créés</h3>
          <div class="kv">
            @for (kv of (activityResource.value()!.plansPerPeriod | keyvalue); track kv.key) {
              <span>{{ kv.key }}</span><span>{{ kv.value }}</span>
            }
          </div>
        }
      </section>
    }
  `,
  styles: `
    .kv { display: grid; grid-template-columns: 1fr auto; gap: 4px 16px; font-size: .92rem; margin-bottom: 4px; }
    .kv span:last-child { text-align: right; font-variant-numeric: tabular-nums; color: var(--muted); }
  `
})
export class StatsPageComponent {
  private readonly api = inject(ApiService);

  readonly period = signal('month');

  // 4 resources distincts pour permettre des reload indépendants (ex. : recharger
  // uniquement l'activité sans refaire les 3 autres appels).
  readonly statsResource = resource({
    loader: () => firstValueFrom(this.api.getStats()),
  });

  readonly collectionResource = resource({
    loader: () => firstValueFrom(this.api.getCollectionProfile()),
  });

  readonly enrichmentResource = resource({
    loader: () => firstValueFrom(this.api.getEnrichmentStats()),
  });

  readonly activityResource = resource({
    params: () => this.period(), // le changement de period() déclenche automatiquement le rechargement
    loader: ({ params: p }) => firstValueFrom(this.api.getActivityTimeline(p)),
  });

  reloadAll(): void {
    this.statsResource.reload();
    this.collectionResource.reload();
    this.enrichmentResource.reload();
    this.activityResource.reload();
  }
}
