import { useSnackbar, type VariantType } from 'notistack';
import { useCallback } from 'react';

/**
 * Hook de notifications (snackbar) avec méthodes typées.
 * Utilise notistack pour afficher des messages en bas à droite.
 * Durée : 5s pour les erreurs, 3s pour les autres types.
 * @returns Objet avec success(), error(), info(), warning().
 */
export function useNotification() {
  const { enqueueSnackbar } = useSnackbar();

  const notify = useCallback(
    (message: string, variant: VariantType = 'default') => {
      enqueueSnackbar(message, { variant, autoHideDuration: variant === 'error' ? 5000 : 3000 });
    },
    [enqueueSnackbar],
  );

  return {
    success: (msg: string) => notify(msg, 'success'),
    error: (msg: string) => notify(msg, 'error'),
    info: (msg: string) => notify(msg, 'info'),
    warning: (msg: string) => notify(msg, 'warning'),
  };
}
