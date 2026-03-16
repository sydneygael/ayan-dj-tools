import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import i18n from '../i18n';

/** État de la langue sélectionnée. */
interface LanguageState {
  /** Langue active : fr ou en. */
  language: 'fr' | 'en';
  /** Change la langue, met à jour i18next et persiste dans localStorage. */
  setLanguage: (lang: 'fr' | 'en') => void;
}

/**
 * Store Zustand de la langue globale.
 * Persisté dans localStorage sous la clé "language".
 * Valeur par défaut : fr (français).
 */
export const useLanguageStore = create<LanguageState>()(
  persist(
    (set) => ({
      language: (i18n.language?.startsWith('en') ? 'en' : 'fr') as 'fr' | 'en',
      setLanguage: (language) => {
        i18n.changeLanguage(language);
        set({ language });
      },
    }),
    { name: 'language' },
  ),
);
