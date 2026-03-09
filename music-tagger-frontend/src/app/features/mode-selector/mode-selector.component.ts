import { Component, inject } from '@angular/core';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { ModeService } from '../../services/mode.service';

@Component({
  selector: 'app-mode-selector',
  standalone: true,
  imports: [MatButtonToggleModule],
  template: `
    <mat-button-toggle-group [value]="modeService.mode()" (change)="modeService.setMode($event.value)" appearance="standard">
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
/** Selecteur de mode operatoire (Plan / Manuel / Auto) affiche dans la toolbar. Delegue au ModeService global. */
export class ModeSelectorComponent {
  protected modeService = inject(ModeService);
}
