import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiService } from '../core/api.service';
import { HarmonicPlaylist, Playlist, SimilarTrackResult } from '../core/models';

@Component({
  standalone: true,
  selector: 'app-playlist-page',
  imports: [CommonModule],
  template: `
    <h1>Playlist & RAG</h1>

    <section class="panel">
      <h2>Génération Playlist</h2>
      <div class="row">
        <label>BPM min <input type="number" [value]="bpmMin()" (input)="bpmMin.set(+$any($event.target).value)" /></label>
        <label>BPM max <input type="number" [value]="bpmMax()" (input)="bpmMax.set(+$any($event.target).value)" /></label>
        <label>Genre <input [value]="genre()" (input)="genre.set($any($event.target).value)" /></label>
        <button (click)="generate()">Générer</button>
      </div>
      @if (playlistError()) {
        <p class="error">{{ playlistError() }}</p>
      }
      @if (playlist()) {
        <h3>{{ playlist()!.name }} ({{ playlist()!.tracks.length }} tracks)</h3>
        <ul>
          @for (track of playlist()!.tracks; track $index) {
            <li>
              {{ track.artist || 'Unknown' }} - {{ track.title || 'Untitled' }}
              @if (track.audioFeatures?.bpm) {
                <span> ({{ track.audioFeatures?.bpm }} BPM)</span>
              }
            </li>
          }
        </ul>
      }
    </section>

    <section class="panel">
      <h2>Mix Harmonique (Camelot)</h2>
      <p class="hint">Roue de Camelot + transitions ±6 BPM — façon Mixed In Key.</p>
      <div class="row">
        <label>BPM min <input type="number" [value]="hBpmMin()" (input)="hBpmMin.set(+$any($event.target).value)" /></label>
        <label>BPM max <input type="number" [value]="hBpmMax()" (input)="hBpmMax.set(+$any($event.target).value)" /></label>
        <label>Genre <input [value]="hGenre()" (input)="hGenre.set($any($event.target).value)" /></label>
        <label>Énergie <input type="number" [value]="hEnergy()" (input)="hEnergy.set(+$any($event.target).value)" min="0" max="1" step="0.1" /></label>
        <label>Tracks <input type="number" [value]="hCount()" (input)="hCount.set(+$any($event.target).value)" min="1" max="100" /></label>
        <button (click)="generateHarmonic()">Générer</button>
      </div>
      @if (harmonicError()) {
        <p class="error">{{ harmonicError() }}</p>
      }
      @if (harmonic(); as h) {
        <h3>{{ h.name }} ({{ h.tracks.length }} tracks)</h3>
        @if (h.stats.totalTracks > 0) {
          <div class="stats">
            <span>BPM moyen : {{ h.stats.avgBpm | number: '1.0-0' }}</span>
            <span>Énergie : {{ h.stats.avgEnergy | number: '1.2-2' }}</span>
            <span>Compatibilité : {{ h.stats.avgTransitionQuality * 100 | number: '1.0-0' }}%</span>
            <span>Transitions parfaites : {{ h.stats.perfectTransitions }}</span>
          </div>
        }
        <ol class="harmonic">
          @for (pt of h.tracks; track pt.position) {
            <li>
              <span class="key" [title]="pt.camelotKey">{{ pt.camelotKey }}</span>
              <span class="track">{{ pt.track.artist || 'Unknown' }} - {{ pt.track.title || 'Untitled' }}</span>
              @if (pt.track.audioFeatures?.bpm) {
                <span class="bpm">{{ pt.track.audioFeatures?.bpm | number: '1.0-0' }} BPM</span>
              }
              @if (pt.transitionType) {
                <span class="transition" [class]="'t-' + pt.transitionType">
                  {{ pt.transitionType }} ({{ pt.transitionQuality * 100 | number: '1.0-0' }}%)
                </span>
              }
            </li>
          }
        </ol>
      }
    </section>

    <section class="panel">
      <h2>Recherche Similaire</h2>
      <div class="row">
        <input [value]="similarQuery()" (input)="similarQuery.set($any($event.target).value)" placeholder="house groovy energetic 128 bpm" />
        <input type="number" [value]="similarLimit()" (input)="similarLimit.set(+$any($event.target).value)" min="1" max="20" />
        <button (click)="searchSimilar()">Rechercher</button>
      </div>
      @if (similarError()) {
        <p class="error">{{ similarError() }}</p>
      }
      <ul>
        @for (item of similarResults(); track $index) {
          <li>{{ item.track.artist }} - {{ item.track.title }} (score {{ item.similarityScore | number: '1.2-2' }})</li>
        }
      </ul>
    </section>
  `,
  styles: `
    .row { margin-bottom: 8px; display: flex; flex-wrap: wrap; gap: 12px; align-items: center; }
    label { display: inline-flex; align-items: center; gap: 6px; }
    input[type="number"] { width: 90px; }
    ul { margin: 6px 0 0; padding-left: 20px; }
    .hint { color: #888; font-size: 0.85em; margin: 0 0 8px; }
    .stats { display: flex; flex-wrap: wrap; gap: 16px; margin: 8px 0; font-size: 0.9em; color: #bbb; }
    ol.harmonic { list-style: none; margin: 8px 0 0; padding: 0; }
    ol.harmonic li { display: flex; align-items: center; gap: 10px; padding: 4px 0; border-bottom: 1px solid #2a2a2a; }
    .key { display: inline-block; min-width: 38px; text-align: center; font-weight: 700; padding: 2px 6px; border-radius: 4px; background: #1565c0; color: #fff; }
    .track { flex: 1; }
    .bpm { color: #888; font-size: 0.85em; }
    .transition { font-size: 0.75em; padding: 1px 6px; border-radius: 3px; }
    .t-PERFECT_MATCH { background: #2e7d32; color: #fff; }
    .t-ADJACENT_KEY { background: #558b2f; color: #fff; }
    .t-MODE_CHANGE { background: #ef6c00; color: #fff; }
    .t-JUMP { background: #c62828; color: #fff; }
  `
})
export class PlaylistPageComponent {
  private readonly api = inject(ApiService);

  readonly playlist = signal<Playlist | null>(null);
  readonly playlistError = signal<string | null>(null);
  readonly similarResults = signal<SimilarTrackResult[]>([]);
  readonly similarError = signal<string | null>(null);

  readonly bpmMin = signal(120);
  readonly bpmMax = signal(145);
  readonly genre = signal('');
  readonly similarQuery = signal('');
  readonly similarLimit = signal(5);

  readonly harmonic = signal<HarmonicPlaylist | null>(null);
  readonly harmonicError = signal<string | null>(null);
  readonly hBpmMin = signal(120);
  readonly hBpmMax = signal(130);
  readonly hGenre = signal('');
  readonly hEnergy = signal(0.6);
  readonly hCount = signal(25);

  async generate(): Promise<void> {
    this.playlistError.set(null);
    try {
      const response = await firstValueFrom(this.api.generatePlaylist(this.bpmMin(), this.bpmMax(), this.genre()));
      this.playlist.set(response);
    } catch (error: unknown) {
      this.playlistError.set(this.errorToMessage(error));
    }
  }

  async generateHarmonic(): Promise<void> {
    this.harmonicError.set(null);
    try {
      const response = await firstValueFrom(
        this.api.generateHarmonicPlaylist(this.hBpmMin(), this.hBpmMax(), this.hGenre(), this.hEnergy(), this.hCount())
      );
      this.harmonic.set(response);
    } catch (error: unknown) {
      this.harmonicError.set(this.errorToMessage(error));
    }
  }

  async searchSimilar(): Promise<void> {
    const query = this.similarQuery().trim();
    if (!query) {
      this.similarError.set('Requête vide');
      return;
    }
    this.similarError.set(null);
    try {
      const response = await firstValueFrom(this.api.findSimilarTracks(query, this.similarLimit()));
      this.similarResults.set(response);
    } catch (error: unknown) {
      this.similarError.set(this.errorToMessage(error));
      this.similarResults.set([]);
    }
  }

  private errorToMessage(error: unknown): string {
    if (typeof error === 'string') {
      return error;
    }
    if (error instanceof Error) {
      return error.message;
    }
    return 'Erreur';
  }
}
