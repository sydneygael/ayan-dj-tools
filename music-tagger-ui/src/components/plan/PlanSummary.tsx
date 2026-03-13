import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Chip from '@mui/material/Chip';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';
import type { TaggingPlan } from '../../types/types';

const STATUS_LABELS: Record<string, string> = {
  DRAFT: 'Brouillon',
  APPROVED: 'Approuve',
  EXECUTING: 'En cours',
  COMPLETED: 'Termine',
  FAILED: 'Echoue',
};

const STATUS_COLORS: Record<string, 'default' | 'primary' | 'warning' | 'success' | 'error'> = {
  DRAFT: 'default',
  APPROVED: 'primary',
  EXECUTING: 'warning',
  COMPLETED: 'success',
  FAILED: 'error',
};

interface Props {
  plan: TaggingPlan;
}

/**
 * En-tête résumé d'un plan de tagging.
 * Affiche l'ID tronqué du plan, son statut (avec chip coloré),
 * la date de création, et les compteurs (fichiers, tags manquants, opérations).
 */
export default function PlanSummary({ plan }: Props) {
  return (
    <Card variant="outlined">
      <CardContent>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
          <Typography variant="subtitle1" fontWeight={600}>
            Plan {plan.planId.slice(0, 8)}...
          </Typography>
          <Chip
            label={STATUS_LABELS[plan.status] ?? plan.status}
            color={STATUS_COLORS[plan.status] ?? 'default'}
            size="small"
          />
        </Box>
        <Typography variant="body2" color="text.secondary">
          Cree le {new Date(plan.createdAt).toLocaleString('fr-FR')}
        </Typography>
        <Box sx={{ display: 'flex', gap: 3, mt: 1 }}>
          <Typography variant="body2">{plan.totalFiles} fichiers</Typography>
          <Typography variant="body2">{plan.filesWithMissingTags} avec tags manquants</Typography>
          <Typography variant="body2">{plan.operations.length} operations</Typography>
        </Box>
      </CardContent>
    </Card>
  );
}
