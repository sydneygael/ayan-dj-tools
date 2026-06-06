import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiService } from '../core/api.service';
import { ExportTrack, HarmonicPlaylist, Playlist, SimilarTrackResult } from '../core/models';

@Component({
  standalone: true,
  selector: 'app-playlist-page',
  imports: [CommonModule],
  template: `
    <h1>Playlist & RAG</h1>

    <section class="panel">
      <h2>Playlist Thématique</h2>
      <p class="hint">Arc narratif (intro → montée → peak → outro) basé sur les paroles analysées.</p>
      <div class="row">
        <input class="theme-input" [value]="tTheme()" (input)="tTheme.set($any($event.target).value)"
               placeholder="Ex : liberté danse africa · été festif soleil · mélancolie pluie" />
        <label>BPM min <input type="number" [value]="tBpmMin()" (input)="tBpmMin.set(+$any($event.target).value)" /></label>
        <label>BPM max <input type="number" [value]="tBpmMax()" (input)="tBpmMax.set(+$any($event.target).value)" /></label>
        <label>Tracks <input type="number" [value]="tCount()" (input)="tCount.set(+$any($event.target).value)" min="4" max="50" /></label>
        <button (click)="generateThematic()" [disabled]="!tTheme().trim()">Générer</button>
      </div>
      @if (thematicError()) {
        <p class="error">{{ thematicError() }}</p>
      }
      @if (thematic()) {
        <div class="playlist-header">
          <h3>{{ thematic()!.name }} ({{ thematic()!.tracks.length }} tracks)</h3>
          <button class="export-btn" (click)="exportM3u(thematic()!.name, toExportTracks(thematic()!.tracks))">
            ⬇ Export M3U
          </button>
        </div>
        <div class="arc-legend">
          <span class="arc-intro">Intro</span>
          <span class="arc-build">Montée</span>
          <span class="arc-peak">Peak</span>
          <span class="arc-outro">Outro</span>
        </div>
        <ol class="thematic">
          @for (track of thematic()!.tracks; track $index; let i = $index) {
            <li [class]="arcClass(i, thematic()!.tracks.length)">
              <span class="pos">{{ i + 1 }}</span>
              <span class="track">{{ track.artist || 'Unknown' }} - {{ track.title || 'Untitled' }}</span>
              @if (track.audioFeatures?.bpm) {
                <span class="bpm">{{ track.audioFeatures?.bpm | number:'1.0-0' }} BPM</span>
              }
              @if (track.audioFeatures?.energy != null) {
                <span class="energy">⚡ {{ track.audioFeatures?.energy | number:'1.2-2' }}</span>
              }
            </li>
          }
        </ol>
      }
    </section>

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
        <div class="playlist-header">
          <h3>{{ playlist()!.name }} ({{ playlist()!.tracks.length }} tracks)</h3>
          <button class="export-btn" (click)="exportM3u(playlist()!.name, toExportTracks(playlist()!.tracks))">
            ⬇ Export M3U
          </button>
        </div>
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
        <div class="playlist-header">
          <h3>{{ h.name }} ({{ h.tracks.length }} tracks)</h3>
          <button class="export-btn" (click)="exportM3u(h.name, toExportTracksFromHarmonic(h.tracks))">
            ⬇ Export M3U
          </button>
        </div>
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
    .theme-input { flex: 1; min-width: 260px; }
    .arc-legend { display: flex; gap: 8px; margin: 8px 0 4px; font-size: .76rem; }
    .arc-legend span { padding: 2px 8px; border-radius: 3px; font-weight: 600; }
    ol.thematic { list-style: none; margin: 0; padding: 0; }
    ol.thematic li { display: flex; align-items: center; gap: 10px; padding: 4px 8px; border-left: 4px solid transparent; border-bottom: 1px solid #222; }
    .pos { min-width: 22px; text-align: right; color: #666; font-size: .82rem; }
    .energy { font-size: .78rem; color: #888; }
    li.arc-intro  { border-left-color: #1565c0; }
    li.arc-build  { border-left-color: #558b2f; }
    li.arc-peak   { border-left-color: #c62828; }
    li.arc-outro  { border-left-color: #6a1b9a; }
    .arc-legend .arc-intro  { background: #1565c0; color: #fff; }
    .arc-legend .arc-build  { background: #558b2f; color: #fff; }
    .arc-legend .arc-peak   { background: #c62828; color: #fff; }
    .arc-legend .arc-outro  { background: #6a1b9a; color: #fff; }
    .playlist-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
    .playlist-header h3 { margin: 0; }
    .export-btn { background: transparent; border: 1px solid #555; color: #ccc; padding: 4px 12px; border-radius: 4px; cursor: pointer; font-size: .82rem; }
    .export-btn:hover { background: #333; color: #fff; }
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

  readonly thematic = signal<Playlist | null>(null);
  readonly thematicError = signal<string | null>(null);
  readonly tTheme = signal('');
  readonly tBpmMin = signal(0);
  readonly tBpmMax = signal(300);
  readonly tCount = signal(12);

  readonly harmonic = signal<HarmonicPlaylist | null>(null);
  readonly harmonicError = signal<string | null>(null);
  readonly hBpmMin = signal(120);
  readonly hBpmMax = signal(130);
  readonly hGenre = signal('');
  readonly hEnergy = signal(0.6);
  readonly hCount = signal(25);

  /** Génère une playlist thématique et met à jour le signal `thematic`. */
  async generateThematic(): Promise<void> {
    this.thematicError.set(null);
    try {
      this.thematic.set(await firstValueFrom(
        this.api.generateThematicPlaylist(this.tTheme(), this.tBpmMin(), this.tBpmMax(), this.tCount())
      ));
    } catch (error: unknown) {
      this.thematicError.set(this.errorToMessage(error));
    }
  }

  /**
   * Retourne la classe CSS d'arc pour un morceau à la position `index` dans une liste de `total` morceaux.
   * Arc : intro (1er quartile) → build → peak → outro (dernier quartile).
   */
  arcClass(index: number, total: number): string {
    const q = Math.floor(total / 4);
    if (index < q)     return 'arc-intro';
    if (index < 2 * q) return 'arc-build';
    if (index < 3 * q) return 'arc-peak';
    return 'arc-outro';
  }

  /** Génère une playlist loop-mixing et met à jour le signal `playlist`. */
  async generate(): Promise<void> {
    this.playlistError.set(null);
    try {
      this.playlist.set(await firstValueFrom(
        this.api.generatePlaylist(this.bpmMin(), this.bpmMax(), this.genre())
      ));
    } catch (error: unknown) {
      this.playlistError.set(this.errorToMessage(error));
    }
  }

  /** Génère une playlist harmonique Camelot et met à jour le signal `harmonic`. */
  async generateHarmonic(): Promise<void> {
    this.harmonicError.set(null);
    try {
      this.harmonic.set(await firstValueFrom(
        this.api.generateHarmonicPlaylist(this.hBpmMin(), this.hBpmMax(), this.hGenre(), this.hEnergy(), this.hCount())
      ));
    } catch (error: unknown) {
      this.harmonicError.set(this.errorToMessage(error));
    }
  }

  /** Recherche des morceaux similaires via RAG et met à jour `similarResults`. */
  async searchSimilar(): Promise<void> {
    const query = this.similarQuery().trim();
    if (!query) { this.similarError.set('Requête vide'); return; }
    this.similarError.set(null);
    try {
      this.similarResults.set(await firstValueFrom(
        this.api.findSimilarTracks(query, this.similarLimit())
      ));
    } catch (error: unknown) {
      this.similarError.set(this.errorToMessage(error));
      this.similarResults.set([]);
    }
  }

  /** Génère le fichier M3U côté backend et le propose au téléchargement. */
  async exportM3u(name: string, tracks: ExportTrack[]): Promise<void> {
    try {
      const content = await firstValueFrom(this.api.exportPlaylistM3u(name, tracks));
      this.downloadFile(content, `${name}.m3u`, 'text/plain');
    } catch {
      // silencieux — l'utilisateur voit son navigateur réagir ou non
    }
  }

  /** Projette une liste de tracks plats vers le format d'export M3U. */
  toExportTracks(tracks: { artist?: string | null; title?: string | null; durationMs?: number | null }[]): ExportTrack[] {
    return tracks.map(t => ({ artist: t.artist, title: t.title, durationMs: t.durationMs }));
  }

  /** Projette une liste de PlaylistTrack harmoniques vers le format d'export M3U. */
  toExportTracksFromHarmonic(tracks: { track: { artist?: string | null; title?: string | null; durationMs?: number | null } }[]): ExportTrack[] {
    return tracks.map(pt => ({ artist: pt.track.artist, title: pt.track.title, durationMs: pt.track.durationMs }));
  }

  /** Déclenche un téléchargement navigateur via un lien temporaire. */
  private downloadFile(content: string, filename: string, type: string): void {
    const blob = new Blob([content], { type });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  }

  private errorToMessage(error: unknown): string {
    if (typeof error === 'string') return error;
    if (error instanceof Error)    return error.message;
    return 'Erreur';
  }
}
