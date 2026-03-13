import { useCallback, useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import Typography from '@mui/material/Typography';
import CheckIcon from '@mui/icons-material/Check';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import DeleteIcon from '@mui/icons-material/Delete';
import { getPlan, approvePlan, executePlan, deletePlan } from '../../api/planApi';
import { useNotification } from '../../utils/notifications';
import type { TaggingPlan, BatchApplyResult } from '../../types/types';
import { OperationStatus, PlanStatus } from '../../types/types';
import PlanSummary from './PlanSummary';
import OperationCard from './OperationCard';
import PlanProgress from './PlanProgress';
import ConfirmDialog from '../dialogs/ConfirmDialog';

/**
 * Page de revue d'un plan de tagging (route /plan/:id).
 * Charge le plan depuis l'API, permet d'approuver/rejeter chaque opération individuellement,
 * d'approuver toutes les opérations d'un coup, d'exécuter le plan (avec dialog de confirmation),
 * ou de le supprimer. Affiche la progression pendant l'exécution et les résultats après.
 */
export default function PlanReviewPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const notify = useNotification();
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
      setError('Impossible de charger le plan');
    } finally {
      setLoading(false);
    }
  }, [id]);

  // Chargement du plan au montage et à chaque changement d'ID dans l'URL.
  // loadPlan est mémorisé via useCallback et dépend de `id` (useParams).
  useEffect(() => {
    loadPlan();
  }, [loadPlan]);

  const handleApproveAll = async () => {
    if (!id) return;
    try {
      setPlan(await approvePlan(id));
      notify.success('Plan approuve');
    } catch {
      notify.error("Erreur lors de l'approbation");
    }
  };

  const handleExecute = async () => {
    if (!id) return;
    setConfirmOpen(null);
    setExecuting(true);
    try {
      const res = await executePlan(id);
      setResult(res);
      notify.success(`Execution terminee : ${res.successCount}/${res.totalFiles} succes`);
      await loadPlan();
    } catch {
      notify.error("Erreur lors de l'execution");
    } finally {
      setExecuting(false);
    }
  };

  const handleDelete = async () => {
    if (!id) return;
    setConfirmOpen(null);
    try {
      await deletePlan(id);
      notify.success('Plan supprime');
      navigate('/');
    } catch {
      notify.error('Erreur lors de la suppression');
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
        <Typography color="error">{error ?? 'Plan introuvable'}</Typography>
      </Box>
    );
  }

  const canApprove = plan.status === PlanStatus.DRAFT;
  const canExecute = plan.status === PlanStatus.APPROVED;

  return (
    <Box sx={{ maxWidth: 800, mx: 'auto' }}>
      <PlanSummary plan={plan} />

      <Box sx={{ display: 'flex', gap: 1, my: 2 }}>
        {canApprove && (
          <Button variant="contained" startIcon={<CheckIcon />} onClick={handleApproveAll}>
            Tout approuver
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
            Executer
          </Button>
        )}
        <Button
          variant="outlined"
          color="error"
          startIcon={<DeleteIcon />}
          onClick={() => setConfirmOpen('delete')}
        >
          Supprimer
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
          title: 'Executer le plan',
          message: `Appliquer les tags sur ${plan.operations.length} fichier(s) ?`,
          confirmLabel: 'Executer',
          warn: true,
        }}
        onConfirm={handleExecute}
      />
      <ConfirmDialog
        open={confirmOpen === 'delete'}
        onClose={() => setConfirmOpen(null)}
        data={{
          title: 'Supprimer le plan',
          message: 'Cette action est irreversible.',
          confirmLabel: 'Supprimer',
          warn: true,
        }}
        onConfirm={handleDelete}
      />
    </Box>
  );
}
