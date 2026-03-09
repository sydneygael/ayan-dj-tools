import { Injectable, computed, signal } from '@angular/core';
import { OperatingMode } from '../models/types';

/**
 * Service global de gestion du mode operatoire (PLAN, MANUAL, APPLY).
 * Le mode initial est restaure depuis localStorage, avec PLAN par defaut.
 */
@Injectable({ providedIn: 'root' })
export class ModeService {
  // Restaure le mode persiste dans localStorage, ou PLAN par defaut
  readonly mode = signal<OperatingMode>(
    (localStorage.getItem('defaultMode') as OperatingMode) || 'PLAN',
  );

  readonly isPlan = computed(() => this.mode() === 'PLAN');
  readonly isManual = computed(() => this.mode() === 'MANUAL');
  readonly isApply = computed(() => this.mode() === 'APPLY');

  setMode(mode: OperatingMode): void {
    this.mode.set(mode);
  }
}
