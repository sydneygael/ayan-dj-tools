import { Component, ElementRef, ViewChild, inject, signal, computed, effect } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatSliderModule } from '@angular/material/slider';
import { FormsModule } from '@angular/forms';
import { FileSelectionService } from '../../services/file-selection.service';

function formatTime(seconds: number): string {
  if (!isFinite(seconds)) return '0:00';
  const m = Math.floor(seconds / 60);
  const s = Math.floor(seconds % 60);
  return `${m}:${s.toString().padStart(2, '0')}`;
}

@Component({
  selector: 'app-audio-player',
  standalone: true,
  imports: [MatIconModule, MatButtonModule, MatSliderModule, FormsModule],
  template: `
    @if (selectedFile()) {
      <div class="player">
        <audio #audioRef
          [src]="audioSrc()"
          (timeupdate)="onTimeUpdate()"
          (loadedmetadata)="onMetadata()"
          (ended)="isPlaying.set(false)">
        </audio>

        <div class="filename">{{ filename() }}</div>

        <div class="controls">
          <button mat-icon-button (click)="togglePlay()">
            <mat-icon>{{ isPlaying() ? 'pause' : 'play_arrow' }}</mat-icon>
          </button>
          <span class="time">{{ formatTime(currentTime()) }}</span>
          <mat-slider class="progress-slider" [max]="duration() || 1" step="1" discrete="false">
            <input matSliderThumb [(ngModel)]="currentTimeModel" (ngModelChange)="seek($event)" />
          </mat-slider>
          <span class="time">{{ formatTime(duration()) }}</span>
          <mat-icon class="vol-icon">volume_up</mat-icon>
          <mat-slider class="vol-slider" min="0" max="1" step="0.05">
            <input matSliderThumb [(ngModel)]="volumeModel" (ngModelChange)="setVolume($event)" />
          </mat-slider>
        </div>
      </div>
    }
  `,
  styles: `
    .player {
      border-top: 1px solid var(--mat-sys-outline-variant);
      padding: 8px 12px;
      background: var(--mat-sys-surface-container-low);
    }
    .filename {
      font-size: 0.75rem;
      color: var(--mat-sys-on-surface-variant);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      margin-bottom: 4px;
    }
    .controls {
      display: flex;
      align-items: center;
      gap: 4px;
    }
    .time {
      font-size: 0.7rem;
      font-variant-numeric: tabular-nums;
      white-space: nowrap;
      color: var(--mat-sys-on-surface-variant);
    }
    .progress-slider {
      flex: 1;
    }
    .vol-slider {
      width: 60px;
    }
    .vol-icon {
      font-size: 18px;
      width: 18px;
      height: 18px;
      color: var(--mat-sys-on-surface-variant);
    }
  `,
})
export class AudioPlayerComponent {
  @ViewChild('audioRef') audioRef!: ElementRef<HTMLAudioElement>;

  private fileService = inject(FileSelectionService);
  protected selectedFile = this.fileService.selectedSingleFile;

  protected isPlaying = signal(false);
  protected currentTime = signal(0);
  protected duration = signal(0);
  protected currentTimeModel = 0;
  protected volumeModel = 1;

  protected readonly formatTime = formatTime;

  protected filename = computed(() => {
    const f = this.selectedFile();
    if (!f) return '';
    return f.split(/[/\\]/).pop() ?? f;
  });

  protected audioSrc = computed(() => {
    const f = this.selectedFile();
    if (!f) return '';
    return f.startsWith('file://') ? f : `file:///${f.replace(/\\/g, '/')}`;
  });

  constructor() {
    effect(() => {
      this.selectedFile();
      this.isPlaying.set(false);
      this.currentTime.set(0);
      this.duration.set(0);
      this.currentTimeModel = 0;
    });
  }

  togglePlay(): void {
    const audio = this.audioRef?.nativeElement;
    if (!audio) return;
    if (this.isPlaying()) {
      audio.pause();
      this.isPlaying.set(false);
    } else {
      audio.play().then(() => this.isPlaying.set(true)).catch(() => {});
    }
  }

  onTimeUpdate(): void {
    const audio = this.audioRef?.nativeElement;
    if (!audio) return;
    this.currentTime.set(audio.currentTime);
    this.currentTimeModel = audio.currentTime;
  }

  onMetadata(): void {
    const audio = this.audioRef?.nativeElement;
    if (!audio) return;
    this.duration.set(audio.duration);
  }

  seek(value: number): void {
    const audio = this.audioRef?.nativeElement;
    if (audio) audio.currentTime = value;
  }

  setVolume(value: number): void {
    const audio = this.audioRef?.nativeElement;
    if (audio) audio.volume = value;
  }
}
