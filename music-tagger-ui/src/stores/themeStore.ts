import { create } from 'zustand';
import { persist } from 'zustand/middleware';

/** État du thème visuel de l'application. */
interface ThemeState {
  /** true = thème sombre (défaut), false = thème clair. */
  isDark: boolean;
  /** Bascule entre thème sombre et clair. */
  toggle: () => void;
}

/**
 * Store Zustand du thème (sombre/clair).
 * Persisté dans localStorage sous la clé "theme".
 * Par défaut le thème sombre est activé (isDark: true).
 */
export const useThemeStore = create<ThemeState>()(
  persist(
    (set) => ({
      isDark: true,
      toggle: () => set((s) => ({ isDark: !s.isDark })),
    }),
    { name: 'theme' },
  ),
);
