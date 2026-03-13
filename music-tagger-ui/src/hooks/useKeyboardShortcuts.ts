import { useEffect } from 'react';
import { useNavigate } from 'react-router';
import { useModeStore } from '../stores/modeStore';
import { useFileStore } from '../stores/fileStore';

/**
 * Hook de raccourcis clavier globaux.
 * Écoute les événements keydown sur le document et déclenche les actions correspondantes.
 * Ignore les événements provenant de champs de saisie (INPUT/TEXTAREA).
 * @param onShowHelp - Callback appelé quand la touche "?" est pressée (ouvre la dialog d'aide).
 */
export function useKeyboardShortcuts(onShowHelp: () => void) {
  const navigate = useNavigate();
  const setMode = useModeStore((s) => s.setMode);
  const selectFiles = useFileStore((s) => s.selectFiles);

  // Enregistrement global des raccourcis clavier au montage.
  // Ignore les événements provenant d'un champ de saisie (INPUT/TEXTAREA).
  // Raccourcis : ? = aide, Ctrl+P/M/A = modes, Ctrl+H/S/,/O = navigation/actions.
  // Le cleanup retire le listener pour éviter les doublons après re-render.
  useEffect(() => {
    function handler(e: KeyboardEvent) {
      const tag = (e.target as HTMLElement)?.tagName;
      if (tag === 'INPUT' || tag === 'TEXTAREA') return;

      if (e.key === '?' && !e.ctrlKey) {
        e.preventDefault();
        onShowHelp();
        return;
      }

      if (!e.ctrlKey) return;

      switch (e.key.toLowerCase()) {
        case 'p':
          e.preventDefault();
          setMode('PLAN');
          break;
        case 'm':
          e.preventDefault();
          setMode('MANUAL');
          break;
        case 'a':
          e.preventDefault();
          setMode('APPLY');
          break;
        case 'h':
          e.preventDefault();
          navigate('/history');
          break;
        case 's':
          e.preventDefault();
          navigate('/stats');
          break;
        case ',':
          e.preventDefault();
          navigate('/settings');
          break;
        case 'o':
          e.preventDefault();
          selectFiles();
          break;
      }
    }

    document.addEventListener('keydown', handler);
    return () => document.removeEventListener('keydown', handler);
  }, [navigate, setMode, selectFiles, onShowHelp]);
}
