import { useCallback, useEffect, useState } from 'react';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import CircularProgress from '@mui/material/CircularProgress';
import LinearProgress from '@mui/material/LinearProgress';
import Typography from '@mui/material/Typography';
import CheckIcon from '@mui/icons-material/Check';
import CloseIcon from '@mui/icons-material/Close';
import HistoryIcon from '@mui/icons-material/History';
import RefreshIcon from '@mui/icons-material/Refresh';
import { useNavigate } from 'react-router';
import { useTranslation } from 'react-i18next';
import { getCurrentOperation, confirmOperation } from '../../api/planApi';
import { useNotification } from '../../utils/notifications';
import { usePlanProgress } from '../../hooks/usePlanProgress';
import type { TaggingPlan, TagOperation } from '../../types/types';
import { OperationStatus } from '../../types/types';
import OperationCard from './OperationCard';

interface Props {
  plan: TaggingPlan;
  onDeleted: () => void;
}

/**
 * Vue du mode MANUAL : affiche les opérations une par une.
 * L'utilisateur approuve ou rejette chaque fichier séquentiellement.
 */
export default function ManualModeView({ plan }: Props) {
  const navigate = useNavigate();
  const notify = useNotification();
  const { t } = useTranslation();
  const [currentOp, setCurrentOp] = useState<TagOperation | null>(null);
  const [currentIndex, setCurrentIndex] = useState(plan.currentIndex);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [complete, setComplete] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { events } = usePlanProgress(plan.planId);

  const total = plan.operations.length;
  const progress = total > 0 ? (currentIndex / total) * 100 : 0;

  const loadCurrentOp = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const op = await getCurrentOperation(plan.planId);
      setCurrentOp(op);
    } catch {
      setError(t('manual.loadError'));
    } finally {
      setLoading(false);
    }
  }, [plan.planId, t]);

  // Charge l'opération courante à chaque avancée de l'index (après approve/reject).
  // Quand currentIndex atteint total, on passe en état "complete" au lieu de fetcher.
  // Se re-déclenche après chaque handleConfirm qui incrémente currentIndex.
  useEffect(() => {
    if (currentIndex >= total) {
      setComplete(true);
      setLoading(false);
    } else {
      loadCurrentOp();
    }
  }, [currentIndex, total, loadCurrentOp]);

  const handleConfirm = async (approved: boolean) => {
    setSubmitting(true);
    try {
      await confirmOperation(plan.planId, currentIndex, approved);
      notify.success(approved ? t('manual.operationApproved') : t('manual.operationRejected'));
      setCurrentIndex((i) => i + 1);
    } catch {
      notify.error(t('manual.confirmError'));
    } finally {
      setSubmitting(false);
    }
  };

  // Count results from progress events
  const successCount = events.filter((e) => e.status === OperationStatus.APPLIED).length;
  const errorCount = events.filter((e) => e.status === OperationStatus.ERROR).length;

  if (error) {
    return (
      <Box sx={{ mt: 3 }}>
        <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>
        <Box sx={{ textAlign: 'center' }}>
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={loadCurrentOp}
          >
            {t('common.retry')}
          </Button>
        </Box>
      </Box>
    );
  }

  if (complete) {
    return (
      <Box sx={{ mt: 3 }}>
        <Card variant="outlined">
          <CardContent sx={{ textAlign: 'center' }}>
            <Typography variant="h6" gutterBottom>{t('manual.processingComplete')}</Typography>
            <Box sx={{ display: 'flex', justifyContent: 'center', gap: 3, mb: 2 }}>
              <Typography color="success.main">{successCount} {t('common.success')}</Typography>
              {errorCount > 0 && <Typography color="error.main">{errorCount} {t('common.errors')}</Typography>}
              <Typography color="text.secondary">{total} {t('common.total')}</Typography>
            </Box>
            <Button
              variant="outlined"
              startIcon={<HistoryIcon />}
              onClick={() => navigate('/history')}
            >
              {t('common.viewHistory')}
            </Button>
          </CardContent>
        </Card>
      </Box>
    );
  }

  return (
    <Box sx={{ mt: 2 }}>
      <Box sx={{ mb: 2 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
          <Typography variant="subtitle2">
            {t('manual.fileProgress', { current: currentIndex + 1, total })}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {Math.round(progress)}%
          </Typography>
        </Box>
        <LinearProgress variant="determinate" value={progress} />
      </Box>

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
          <CircularProgress />
        </Box>
      ) : currentOp ? (
        <>
          <OperationCard operation={currentOp} onApprove={() => {}} onReject={() => {}} />
          <Box sx={{ display: 'flex', gap: 2, justifyContent: 'center', mt: 2 }}>
            <Button
              variant="outlined"
              color="error"
              startIcon={<CloseIcon />}
              onClick={() => handleConfirm(false)}
              disabled={submitting}
            >
              {t('plan.reject')}
            </Button>
            <Button
              variant="contained"
              color="success"
              startIcon={<CheckIcon />}
              onClick={() => handleConfirm(true)}
              disabled={submitting}
            >
              {t('plan.approve')}
            </Button>
          </Box>
        </>
      ) : null}
    </Box>
  );
}
