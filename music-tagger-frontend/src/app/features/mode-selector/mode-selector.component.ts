import { Component, signal } from '@angular/core';
import { MatButtonToggleModule } from '@angular/material/button-toggle';

export type OperatingMode = 'PLAN' | 'MANUAL' | 'APPLY';

@Component({
  selector: 'app-mode-selector',
  standalone: true,
  imports: [MatButtonToggleModule],
  template: `
    <mat-button-toggle-group [value]="mode()" (change)="mode.set($event.value)" appearance="standard">
      <mat-button-toggle value="PLAN">Plan</mat-button-toggle>
      <mat-button-toggle value="MANUAL">Manuel</mat-button-toggle>
      <mat-button-toggle value="APPLY">Auto</mat-button-toggle>
    </mat-button-toggle-group>
  `,
  styles: `
    :host {
      display: flex;
      align-items: center;
    }
  `,
})
export class ModeSelectorComponent {
  readonly mode = signal<OperatingMode>('PLAN');
}
