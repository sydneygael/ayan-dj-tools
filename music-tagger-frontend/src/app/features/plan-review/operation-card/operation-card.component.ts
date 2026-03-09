import { Component, input, output } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TagOperation, OperationStatus } from '../../../models/types';

@Component({
  selector: 'app-operation-card',
  standalone: true,
  imports: [MatCardModule, MatChipsModule, MatButtonModule, MatIconModule, MatTooltipModule],
  template: `
    <mat-card class="op-card">
      <mat-card-header>
        <mat-card-title class="filename">{{ filename() }}</mat-card-title>
        <mat-chip [class]="'status-' + operation().status.toLowerCase()" class="status-chip">
          {{ operation().status }}
        </mat-chip>
      </mat-card-header>
      <mat-card-content>
        <div class="diff-table">
          <div class="diff-header">
            <span>Tag</span>
            <span>Actuel</span>
            <span>Suggere</span>
          </div>
          @for (entry of allTags(); track entry.key) {
            <div class="diff-row" [class.changed]="entry.current !== entry.suggested">
              <span class="tag-name">{{ entry.key }}</span>
              <span class="old-value">{{ entry.current || '—' }}</span>
              <span class="new-value">{{ entry.suggested || '—' }}</span>
            </div>
          }
        </div>
        @if (operation().message) {
          <p class="message">{{ operation().message }}</p>
        }
      </mat-card-content>
      @if (operation().status === 'PENDING') {
        <mat-card-actions align="end">
          <button mat-button color="warn" (click)="rejected.emit(operation().filepath)"
                  matTooltip="Rejeter cette operation">
            <mat-icon>close</mat-icon> Rejeter
          </button>
          <button mat-flat-button (click)="approved.emit(operation().filepath)"
                  matTooltip="Approuver cette operation">
            <mat-icon>check</mat-icon> Approuver
          </button>
        </mat-card-actions>
      }
    </mat-card>
  `,
  styles: `
    .op-card {
      margin-bottom: 8px;
    }
    mat-card-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }
    .filename {
      font-size: 0.9rem;
      font-family: monospace;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      max-width: 400px;
    }
    .status-chip {
      font-size: 0.75rem;
    }
    .diff-table {
      display: grid;
      grid-template-columns: 120px 1fr 1fr;
      gap: 4px 12px;
      font-size: 0.85rem;
      margin: 12px 0;
    }
    .diff-header {
      display: contents;
      span {
        font-weight: 600;
        color: var(--mat-sys-on-surface-variant);
        text-transform: uppercase;
        font-size: 0.7rem;
        letter-spacing: 0.05em;
        padding-bottom: 4px;
        border-bottom: 1px solid var(--mat-sys-outline-variant);
      }
    }
    .diff-row {
      display: contents;
    }
    .tag-name {
      font-weight: 500;
    }
    .old-value {
      color: var(--mat-sys-on-surface-variant);
    }
    .changed .old-value {
      color: #f44336;
      text-decoration: line-through;
    }
    .changed .new-value {
      color: #4caf50;
      font-weight: 500;
    }
    .message {
      font-size: 0.8rem;
      color: var(--mat-sys-on-surface-variant);
      font-style: italic;
    }
    .status-pending { --mat-chip-label-text-color: #ff9800; }
    .status-approved { --mat-chip-label-text-color: #4caf50; }
    .status-rejected { --mat-chip-label-text-color: #f44336; }
    .status-applied { --mat-chip-label-text-color: #66bb6a; }
    .status-failed { --mat-chip-label-text-color: #f44336; }
  `,
})
/**
 * Carte d'une operation de tagging — affiche le diff (tags actuels vs suggeres)
 * et les boutons approuver/rejeter quand l'operation est PENDING.
 */
export class OperationCardComponent {
  readonly operation = input.required<TagOperation>();
  readonly approved = output<string>();
  readonly rejected = output<string>();

  filename(): string {
    const fp = this.operation().filepath;
    return fp.split(/[/\\]/).pop() || fp;
  }

  /** Fusionne les cles de currentTags et suggestedTags pour afficher le diff complet. */
  allTags(): { key: string; current: string; suggested: string }[] {
    const op = this.operation();
    const keys = new Set([...Object.keys(op.currentTags), ...Object.keys(op.suggestedTags)]);
    return [...keys].map(key => ({
      key,
      current: op.currentTags[key] || '',
      suggested: op.suggestedTags[key] || '',
    }));
  }
}
