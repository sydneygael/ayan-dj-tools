import Box from '@mui/material/Box';
import LinearProgress from '@mui/material/LinearProgress';
import Typography from '@mui/material/Typography';
import { useTranslation } from 'react-i18next';
import type { BatchApplyResult } from '../../types/types';

interface Props {
  result: BatchApplyResult | null;
  total: number;
  executing: boolean;
}

/**
 * Barre de progression de l'exécution d'un plan de tagging.
 * 3 états possibles :
 * - Exécution en cours (executing=true, result=null) : barre indéterminée + message
 * - Résultat disponible (result != null) : barre déterminée avec compteurs succès/échecs/total
 * - Aucune exécution : ne rend rien (return null)
 */
export default function PlanProgress({ result, total, executing }: Props) {
  const { t } = useTranslation();

  if (!executing && !result) return null;

  if (executing && !result) {
    return (
      <Box sx={{ my: 2 }}>
        <LinearProgress />
        <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5 }}>
          {t('plan.executing')}
        </Typography>
      </Box>
    );
  }

  if (!result) return null;

  const pct = total > 0 ? ((result.successCount + result.errorCount) / total) * 100 : 0;

  return (
    <Box sx={{ my: 2 }}>
      <LinearProgress variant="determinate" value={pct} />
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 0.5 }}>
        <Typography variant="caption" color="success.main">
          {result.successCount} {t('common.success')}
        </Typography>
        {result.errorCount > 0 && (
          <Typography variant="caption" color="error.main">
            {result.errorCount} {t('plan.failures')}
          </Typography>
        )}
        <Typography variant="caption" color="text.secondary">
          {result.totalOperations} {t('common.total')}
        </Typography>
      </Box>
    </Box>
  );
}
