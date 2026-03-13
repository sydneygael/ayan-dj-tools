import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { OperatingMode } from '../types/types';

/** État du mode d'opération sélectionné. */
interface ModeState {
  /** Mode actif : PLAN, MANUAL ou APPLY. */
  mode: OperatingMode;
  /** Change le mode et persiste le choix dans localStorage. */
  setMode: (mode: OperatingMode) => void;
}

/**
 * Store Zustand du mode d'opération global.
 * Persisté dans localStorage sous la clé "defaultMode".
 * Valeur par défaut : PLAN (revue par lot avant application).
 */
export const useModeStore = create<ModeState>()(
  persist(
    (set) => ({
      mode: 'PLAN',
      setMode: (mode) => set({ mode }),
    }),
    { name: 'defaultMode' },
  ),
);
