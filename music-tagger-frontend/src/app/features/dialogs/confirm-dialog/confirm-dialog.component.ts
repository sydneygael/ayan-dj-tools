import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { ConfirmDialogData } from '../../../models/types';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content>
      <p>{{ data.message }}</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="false">{{ data.cancelLabel || 'Annuler' }}</button>
      <button mat-flat-button [color]="data.warn ? 'warn' : 'primary'" [mat-dialog-close]="true">
        {{ data.confirmLabel || 'Confirmer' }}
      </button>
    </mat-dialog-actions>
  `,
})
/** Dialog generique de confirmation (titre, message, boutons personnalisables, mode warn optionnel). */
export class ConfirmDialogComponent {
  protected data = inject<ConfirmDialogData>(MAT_DIALOG_DATA);
}
