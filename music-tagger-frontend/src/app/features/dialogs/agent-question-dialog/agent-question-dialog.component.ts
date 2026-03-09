import { Component, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatRadioModule } from '@angular/material/radio';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { FormsModule } from '@angular/forms';
import { AgentQuestion, AgentQuestionResponse } from '../../../models/types';

@Component({
  selector: 'app-agent-question-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule, MatRadioModule, MatCheckboxModule, FormsModule],
  template: `
    <h2 mat-dialog-title>Question de l'agent</h2>
    <mat-dialog-content>
      <p class="question">{{ data.question }}</p>
      @if (data.context) {
        <p class="context">{{ data.context }}</p>
      }
      <mat-radio-group [(ngModel)]="selectedOption" class="options">
        @for (option of data.options; track option) {
          <mat-radio-button [value]="option">{{ option }}</mat-radio-button>
        }
      </mat-radio-group>
      <mat-checkbox [(ngModel)]="applyToSimilar" class="similar-check">
        Appliquer aux pistes similaires
      </mat-checkbox>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Annuler</button>
      <button mat-flat-button [mat-dialog-close]="buildResponse()" [disabled]="!selectedOption()">
        Valider
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    .question {
      font-size: 1rem;
      margin-bottom: 16px;
    }
    .context {
      font-size: 0.85rem;
      color: var(--mat-sys-on-surface-variant);
      margin-bottom: 16px;
    }
    .options {
      display: flex;
      flex-direction: column;
      gap: 8px;
      margin-bottom: 16px;
    }
    .similar-check {
      margin-top: 8px;
    }
  `,
})
/**
 * Dialog affichant une question de l'agent a l'utilisateur (mode MANUAL).
 * L'utilisateur choisit une option parmi les propositions et peut cocher « appliquer aux pistes similaires ».
 * Retourne un AgentQuestionResponse via mat-dialog-close.
 */
export class AgentQuestionDialogComponent {
  protected data = inject<AgentQuestion>(MAT_DIALOG_DATA);
  protected selectedOption = signal<string>('');
  protected applyToSimilar = false;

  /** Construit la reponse a retourner au dialog caller avec l'option selectionnee. */
  buildResponse(): AgentQuestionResponse {
    return {
      questionId: this.data.questionId,
      selectedOption: this.selectedOption(),
      applyToSimilar: this.applyToSimilar,
    };
  }
}
