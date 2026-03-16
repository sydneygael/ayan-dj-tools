import { useCallback, useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import Typography from '@mui/material/Typography';
import CheckIcon from '@mui/icons-material/Check';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import DeleteIcon from '@mui/icons-material/Delete';
import { useTranslation } from 'react-i18next';
import { getPlan, approvePlan, executePlan, deletePlan } from '../../api/planApi';
import { useNotification } from '../../utils/notifications';
import type { TaggingPlan, BatchApplyResult } from '../../types/types';
import { OperationStatus, PlanStatus } from '../../types/types';
import PlanSummary from './PlanSummary';
import OperationCard from './OperationCard';
import PlanProgress from './PlanProgress';
import ConfirmDialog from '../dialogs/ConfirmDialog';
import ManualModeView from './ManualModeView';
import ApplyModeView from './ApplyModeView';

/**
 * Page de revue d'un plan de tagging (route /plan/:id).
 * Dispatche vers ManualModeView ou ApplyModeView selon le mode du plan.
 * En mode PLAN (défaut), affiche la revue complète avec approbation/exécution.
 */
export default function PlanReviewPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const notify = useNotification();
  const { t } = useTranslation();
  const [plan, setPlan] = useState<TaggingPlan | null>(null);
  const [loading, setLoading] = useState(true);
  const [executing, setExecuting] = useState(false);
  const [result, setResult] = useState<BatchApplyResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [confirmOpen, setConfirmOpen] = useState<'execute' | 'delete' | null>(null);

  const loadPlan = useCallback(async () => {
    if (!id) return;
    try {
      setLoading(true);
      setPlan(await getPlan(id));
      setError(null);
    } catch {
      setError(t('plan.loadError'));
    } finally {
      setLoading(false);
    }
  }, [id, t]);

  useEffect(() => {
    loadPlan();
  }, [loadPlan]);

  const handleApproveAll = async () => {
    if (!id) return;
    try {
      setPlan(await approvePlan(id));
      notify.success(t('plan.approved'));
    } catch {
      notify.error(t('plan.approveError'));
    }
  };

  const handleExecute = async () => {
    if (!id) return;
    setConfirmOpen(null);
    setExecuting(true);
    try {
      const res = await executePlan(id);
      setResult(res);
      notify.success(t('plan.executionDone', { success: res.successCount, total: res.totalOperations }));
      await loadPlan();
    } catch {
      notify.error(t('plan.executeError'));
    } finally {
      setExecuting(false);
    }
  };

  const handleDelete = async () => {
    if (!id) return;
    setConfirmOpen(null);
    try {
      await deletePlan(id);
      notify.success(t('plan.deleted'));
      navigate('/');
    } catch {
      notify.error(t('plan.deleteError'));
    }
  };

  const approveOp = (filepath: string) => {
    if (!plan) return;
    setPlan({
      ...plan,
      operations: plan.operations.map((op) =>
        op.filepath === filepath ? { ...op, status: OperationStatus.APPROVED } : op,
      ),
    });
  };

  const rejectOp = (filepath: string) => {
    if (!plan) return;
    setPlan({
      ...plan,
      operations: plan.operations.map((op) =>
        op.filepath === filepath ? { ...op, status: OperationStatus.REJECTED } : op,
      ),
    });
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error || !plan) {
    return (
      <Box sx={{ textAlign: 'center', mt: 8 }}>
        <Typography color="error">{error ?? t('plan.notFound')}</Typography>
      </Box>
    );
  }

  // Dispatch by mode: MANUAL and APPLY get dedicated views
  if (plan.mode === 'MANUAL') {
    return (
      <Box sx={{ maxWidth: 800, mx: 'auto' }}>
        <PlanSummary plan={plan} />
        <ManualModeView plan={plan} onDeleted={() => navigate('/')} />
        <Box sx={{ mt: 2 }}>
          <Button
            variant="outlined"
            color="error"
            startIcon={<DeleteIcon />}
            onClick={() => setConfirmOpen('delete')}
          >
            {t('common.delete')}
          </Button>
        </Box>
        <ConfirmDialog
          open={confirmOpen === 'delete'}
          onClose={() => setConfirmOpen(null)}
          data={{
            title: t('plan.deletePlan'),
            message: t('plan.deleteIrreversible'),
            confirmLabel: t('common.delete'),
            warn: true,
          }}
          onConfirm={handleDelete}
        />
      </Box>
    );
  }

  if (plan.mode === 'APPLY') {
    return (
      <Box sx={{ maxWidth: 800, mx: 'auto' }}>
        <PlanSummary plan={plan} />
        <ApplyModeView plan={plan} onDeleted={() => navigate('/')} />
        <Box sx={{ mt: 2 }}>
          <Button
            variant="outlined"
            color="error"
            startIcon={<DeleteIcon />}
            onClick={() => setConfirmOpen('delete')}
          >
            {t('common.delete')}
          </Button>
        </Box>
        <ConfirmDialog
          open={confirmOpen === 'delete'}
          onClose={() => setConfirmOpen(null)}
          data={{
            title: t('plan.deletePlan'),
            message: t('plan.deleteIrreversible'),
            confirmLabel: t('common.delete'),
            warn: true,
          }}
          onConfirm={handleDelete}
        />
      </Box>
    );
  }

  // Default: PLAN mode — full review UI
  const canApprove = plan.status === PlanStatus.DRAFT;
  const canExecute = plan.status === PlanStatus.APPROVED;

  return (
    <Box sx={{ maxWidth: 800, mx: 'auto' }}>
      <PlanSummary plan={plan} />

      <Box sx={{ display: 'flex', gap: 1, my: 2 }}>
        {canApprove && (
          <Button variant="contained" startIcon={<CheckIcon />} onClick={handleApproveAll}>
            {t('plan.approveAll')}
          </Button>
        )}
        {canExecute && (
          <Button
            variant="contained"
            color="success"
            startIcon={<PlayArrowIcon />}
            onClick={() => setConfirmOpen('execute')}
            disabled={executing}
          >
            {t('plan.execute')}
          </Button>
        )}
        <Button
          variant="outlined"
          color="error"
          startIcon={<DeleteIcon />}
          onClick={() => setConfirmOpen('delete')}
        >
          {t('common.delete')}
        </Button>
      </Box>

      <PlanProgress result={result} total={plan.operations.length} executing={executing} />

      {plan.operations.map((op) => (
        <OperationCard key={op.filepath} operation={op} onApprove={approveOp} onReject={rejectOp} />
      ))}

      <ConfirmDialog
        open={confirmOpen === 'execute'}
        onClose={() => setConfirmOpen(null)}
        data={{
          title: t('plan.executePlan'),
          message: t('plan.applyTagsConfirm', { count: plan.operations.length }),
          confirmLabel: t('plan.execute'),
          warn: true,
        }}
        onConfirm={handleExecute}
      />
      <ConfirmDialog
        open={confirmOpen === 'delete'}
        onClose={() => setConfirmOpen(null)}
        data={{
          title: t('plan.deletePlan'),
          message: t('plan.deleteIrreversible'),
          confirmLabel: t('common.delete'),
          warn: true,
        }}
        onConfirm={handleDelete}
      />
    </Box>
  );
}
