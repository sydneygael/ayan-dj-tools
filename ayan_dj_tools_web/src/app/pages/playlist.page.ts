import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiService } from '../core/api.service';
import { Playlist, SimilarTrackResult } from '../core/models';

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
    .row { margin-bottom: 8px; }
    label { display: inline-flex; align-items: center; gap: 6px; }
    input[type="number"] { width: 90px; }
    ul { margin: 6px 0 0; padding-left: 20px; }
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

  async generate(): Promise<void> {
    this.playlistError.set(null);
    try {
      const response = await firstValueFrom(this.api.generatePlaylist(this.bpmMin(), this.bpmMax(), this.genre()));
      this.playlist.set(response);
    } catch (error: unknown) {
      this.playlistError.set(this.errorToMessage(error));
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
