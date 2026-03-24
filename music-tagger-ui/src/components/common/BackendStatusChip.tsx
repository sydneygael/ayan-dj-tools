import { useEffect } from 'react';
import Chip from '@mui/material/Chip';
import Button from '@mui/material/Button';
import StorageIcon from '@mui/icons-material/Storage';
import { useTranslation } from 'react-i18next';
import { useBackendStore } from '../../stores/backendStore';
import { checkBackendHealth } from '../../api/healthApi';

/**
 * Chip affichant l'état du backend Spring Boot.
 * - En mode Electron : écoute les events IPC poussés par main.ts
 * - En mode navigateur (dev) : poll /actuator/health toutes les 10s
 * Affiche un bouton "Installer" quand une mise à jour est téléchargée.
 */
export default function BackendStatusChip() {
  const { t } = useTranslation();
  const { ready, updateDownloaded, setReady, setUpdateAvailable, setUpdateDownloaded } = useBackendStore();

  useEffect(() => {
    if (window.electron) {
      // Mode Electron : query initiale + subscribe aux events IPC
      window.electron.getBackendStatus().then((s) => setReady(s.ready));

      const unsubStatus = window.electron.onBackendStatus((s) => setReady(s.ready, s.error));
      const unsubUpdate = window.electron.onUpdateAvailable((i) => setUpdateAvailable(i.version));
      const unsubDownloaded = window.electron.onUpdateDownloaded(() => setUpdateDownloaded());

      return () => {
        unsubStatus();
        unsubUpdate();
        unsubDownloaded();
      };
    } else {
      // Mode navigateur : poll toutes les 10s
      const poll = async () => {
        const ok = await checkBackendHealth();
        setReady(ok);
      };
      poll();
      const id = setInterval(poll, 10_000);
      return () => clearInterval(id);
    }
  }, [setReady, setUpdateAvailable, setUpdateDownloaded]);

  return (
    <>
      <Chip
        icon={<StorageIcon sx={{ fontSize: 14 }} />}
        label={ready ? t('backend.ready') : t('backend.notReady')}
        size="small"
        color={ready ? 'success' : 'error'}
        variant="outlined"
        sx={{ height: 24, fontSize: '0.7rem' }}
      />
      {updateDownloaded && (
        <Button
          size="small"
          variant="outlined"
          color="warning"
          onClick={() => window.electron?.installUpdate()}
          sx={{ height: 24, fontSize: '0.7rem', ml: 0.5 }}
        >
          {t('update.install')}
        </Button>
      )}
    </>
  );
}
