import { useEffect } from 'react';
import { useFileStore } from '../stores/fileStore';

/**
 * Connecte les événements IPC Electron au store de l'application.
 * - menu-select-files → déclenche l'ouverture du file picker
 * Doit être appelé une seule fois dans AppContent.
 */
export function useElectronBridge() {
  const selectFiles = useFileStore((s) => s.selectFiles);

  useEffect(() => {
    if (!window.electron) return;
    return window.electron.onMenuSelectFiles(() => selectFiles());
  }, [selectFiles]);
}
