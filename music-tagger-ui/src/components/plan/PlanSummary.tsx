import { useState } from 'react';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Chip from '@mui/material/Chip';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import { useTranslation } from 'react-i18next';
import type { TaggingPlan } from '../../types/types';
import { formatDate } from '../../utils/helpers';

const STATUS_COLORS: Record<string, 'default' | 'primary' | 'warning' | 'success' | 'error'> = {
  DRAFT: 'default',
  READY_FOR_REVIEW: 'warning',
  APPROVED: 'primary',
  APPLYING: 'warning',
  COMPLETED: 'success',
};

interface Props {
  plan: TaggingPlan;
}

/**
 * En-tête résumé d'un plan de tagging.
 * Affiche l'ID tronqué du plan, son statut (avec chip coloré), le mode,
 * la date de création, et les compteurs (fichiers, tags manquants, opérations).
 */
export default function PlanSummary({ plan }: Props) {
  const [copied, setCopied] = useState(false);
  const { t } = useTranslation();

  const copyPlanId = () => {
    navigator.clipboard.writeText(plan.planId);
    setCopied(true);
    setTimeout(() => setCopied(false), 1500);
  };

  const statusLabel = t(`plan.status.${plan.status.toLowerCase()}`);
  const modeLabel = t(`mode.${plan.mode.toLowerCase()}`);

  return (
    <Card variant="outlined">
      <CardContent>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
            <Typography variant="subtitle1" fontWeight={600}>
              Plan {plan.planId.slice(0, 8)}...
            </Typography>
            <Tooltip title={copied ? t('plan.copied') : t('plan.copyId')}>
              <IconButton size="small" onClick={copyPlanId}>
                <ContentCopyIcon sx={{ fontSize: 16 }} />
              </IconButton>
            </Tooltip>
          </Box>
          <Box sx={{ display: 'flex', gap: 1 }}>
            <Chip
              label={modeLabel}
              size="small"
              variant="outlined"
            />
            <Chip
              label={statusLabel}
              color={STATUS_COLORS[plan.status] ?? 'default'}
              size="small"
            />
          </Box>
        </Box>
        <Typography variant="body2" color="text.secondary">
          {t('plan.createdAt', { date: formatDate(plan.createdAt) })}
        </Typography>
        <Box sx={{ display: 'flex', gap: 3, mt: 1 }}>
          <Typography variant="body2">{t('plan.files', { count: plan.totalFiles })}</Typography>
          <Typography variant="body2">{plan.filesWithMissingTags} {t('plan.withMissingTags')}</Typography>
          <Typography variant="body2">{plan.operations.length} {t('plan.operations')}</Typography>
        </Box>
      </CardContent>
    </Card>
  );
}
