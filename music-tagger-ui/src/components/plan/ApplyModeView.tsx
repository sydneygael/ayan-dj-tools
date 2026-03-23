import { useEffect, useRef, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import LinearProgress from '@mui/material/LinearProgress';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import Fade from '@mui/material/Fade';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ErrorIcon from '@mui/icons-material/Error';
import HistoryIcon from '@mui/icons-material/History';
import { useNavigate } from 'react-router';
import { useTranslation } from 'react-i18next';
import { autoExecute } from '../../api/planApi';
import { useNotification } from '../../utils/notifications';
import { usePlanProgress } from '../../hooks/usePlanProgress';
import { extractFilename } from '../../utils/helpers';
import type { TaggingPlan } from '../../types/types';
import { OperationStatus } from '../../types/types';

interface Props {
  plan: TaggingPlan;
  onDeleted: () => void;
}

/**
 * Vue du mode APPLY : exécution automatique avec progression en temps réel.
 * Souscrit au WebSocket puis lance autoExecute. Affiche un log live des événements.
 */
export default function ApplyModeView({ plan }: Props) {
  const navigate = useNavigate();
  const notify = useNotification();
  const { t } = useTranslation();
  const { events, connected } = usePlanProgress(plan.planId);
  const [started, setStarted] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const listEndRef = useRef<HTMLDivElement>(null);

  const total = plan.operations.length;
  const progress = total > 0 ? (events.length / total) * 100 : 0;
  const complete = events.length >= total && total > 0;

  const successCount = events.filter((e) => e.status === OperationStatus.APPLIED).length;
  const errorCount = events.filter((e) => e.status === OperationStatus.ERROR).length;

  // Lance l'exécution automatique dès que la connexion WebSocket STOMP est établie.
  // Attend que connected=true (souscription active) avant d'appeler autoExecute,
  // pour garantir que les événements de progression seront bien reçus.
  // Le flag `started` empêche les appels multiples (strict mode, reconnexions).
  useEffect(() => {
    if (!connected || started) return;
    setStarted(true);
    autoExecute(plan.planId).catch(() => {
      setError(t('apply.autoExecuteError'));
      notify.error(t('apply.autoExecuteError'));
    });
  }, [connected, started, plan.planId, notify, t]);

  // Défilement automatique vers le dernier événement reçu dans le log live.
  // Se déclenche à chaque nouvel événement WebSocket ajouté au tableau events.
  useEffect(() => {
    listEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [events.length]);

  // Notification de fin d'exécution quand tous les événements sont reçus.
  // complete passe à true quand events.length >= total (toutes les opérations traitées).
  useEffect(() => {
    if (complete) {
      notify.success(t('plan.executionDone', { success: successCount, total }));
    }
  }, [complete, successCount, total, notify, t]);

  if (error) {
    return (
      <Box sx={{ mt: 3, textAlign: 'center' }}>
        <Typography color="error" sx={{ mb: 2 }}>{error}</Typography>
        <Button
          variant="outlined"
          onClick={() => { setError(null); setStarted(false); }}
        >
          {t('common.retry')}
        </Button>
      </Box>
    );
  }

  return (
    <Box sx={{ mt: 2 }}>
      <Box sx={{ mb: 2 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
          <Typography variant="subtitle2">
            {complete ? t('apply.executionComplete') : t('apply.autoExecuting')}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {events.length} / {total}
          </Typography>
        </Box>
        <LinearProgress variant="determinate" value={progress} />
      </Box>

      {complete && (
        <Card variant="outlined" sx={{ mb: 2 }}>
          <CardContent sx={{ textAlign: 'center' }}>
            <Typography variant="h6" gutterBottom>{t('apply.result')}</Typography>
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
      )}

      <Card variant="outlined">
        <CardContent sx={{ maxHeight: 400, overflow: 'auto', p: 1 }}>
          <List dense disablePadding>
            {events.map((event, i) => (
              <Fade in timeout={300} key={i}>
              <ListItem disableGutters sx={{ py: 0.25 }}>
                <ListItemIcon sx={{ minWidth: 32 }}>
                  {event.status === OperationStatus.APPLIED ? (
                    <CheckCircleIcon color="success" fontSize="small" />
                  ) : event.status === OperationStatus.ERROR ? (
                    <ErrorIcon color="error" fontSize="small" />
                  ) : (
                    <CircularProgress size={16} />
                  )}
                </ListItemIcon>
                <ListItemText
                  primary={extractFilename(event.filepath)}
                  secondary={event.message}
                  primaryTypographyProps={{ variant: 'body2' }}
                  secondaryTypographyProps={{ variant: 'caption' }}
                />
              </ListItem>
              </Fade>
            ))}
          </List>
          <div ref={listEndRef} />
        </CardContent>
      </Card>
    </Box>
  );
}
