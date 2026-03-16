import { useEffect, useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import CircularProgress from '@mui/material/CircularProgress';
import LinearProgress from '@mui/material/LinearProgress';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import Typography from '@mui/material/Typography';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ErrorIcon from '@mui/icons-material/Error';
import { useTranslation } from 'react-i18next';
import { getStats } from '../../api/statsApi';
import type { StatsReport } from '../../types/types';
import { extractFilename, formatDate } from '../../utils/helpers';

/**
 * Page de statistiques (route /stats).
 * Affiche 4 KPIs (plans créés, tags appliqués, fichiers enrichis, types de tags),
 * un graphique horizontal des tags par type (barres proportionnelles au max),
 * et la liste de l'activité récente (succès/échec, fichier, nombre de modifications, date).
 * Les données sont chargées au montage via l'API GET /api/stats.
 */
export default function StatsPage() {
  const [stats, setStats] = useState<StatsReport | null>(null);
  const [loading, setLoading] = useState(true);
  const { t } = useTranslation();

  useEffect(() => {
    getStats()
      .then(setStats)
      .catch(() => setStats(null))
      .finally(() => setLoading(false));
  }, []);

  const sortedTags = useMemo(() => {
    if (!stats) return [];
    return Object.entries(stats.tagsAppliedByType).sort((a, b) => b[1] - a[1]);
  }, [stats]);

  const maxCount = sortedTags.length > 0 ? sortedTags[0][1] : 1;

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (!stats) {
    return (
      <Typography color="text.secondary" sx={{ textAlign: 'center', mt: 8 }}>
        {t('stats.loadError')}
      </Typography>
    );
  }

  const kpis = [
    { label: t('stats.plansCreated'), value: stats.totalPlansCreated },
    { label: t('stats.tagsApplied'), value: stats.totalTagsApplied },
    { label: t('stats.filesEnriched'), value: stats.totalFilesEnriched },
    { label: t('stats.tagTypes'), value: Object.keys(stats.tagsAppliedByType).length },
  ];

  return (
    <Box sx={{ maxWidth: 800, mx: 'auto' }}>
      <Typography variant="h6" gutterBottom>
        {t('stats.title')}
      </Typography>

      <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: 2, mb: 3 }}>
        {kpis.map((kpi) => (
          <Card key={kpi.label} variant="outlined">
            <CardContent sx={{ textAlign: 'center', py: 1.5, '&:last-child': { pb: 1.5 } }}>
              <Typography variant="h4" color="primary.main">
                {kpi.value}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                {kpi.label}
              </Typography>
            </CardContent>
          </Card>
        ))}
      </Box>

      {sortedTags.length > 0 && (
        <Card variant="outlined" sx={{ mb: 3 }}>
          <CardContent>
            <Typography variant="subtitle2" gutterBottom>
              {t('stats.tagsByType')}
            </Typography>
            {sortedTags.map(([tag, count]) => (
              <Box key={tag} sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5 }}>
                <Typography variant="body2" sx={{ minWidth: 80 }}>
                  {tag}
                </Typography>
                <LinearProgress
                  variant="determinate"
                  value={(count / maxCount) * 100}
                  sx={{ flex: 1, height: 8, borderRadius: 4 }}
                />
                <Typography variant="caption" sx={{ minWidth: 24, textAlign: 'right' }}>
                  {count}
                </Typography>
              </Box>
            ))}
          </CardContent>
        </Card>
      )}

      {stats.recentActivity.length > 0 && (
        <Card variant="outlined">
          <CardContent>
            <Typography variant="subtitle2" gutterBottom>
              {t('stats.recentActivity')}
            </Typography>
            <List dense>
              {stats.recentActivity.map((entry, i) => (
                <ListItem key={i}>
                  <ListItemIcon sx={{ minWidth: 32 }}>
                    {entry.success ? (
                      <CheckCircleIcon fontSize="small" color="success" />
                    ) : (
                      <ErrorIcon fontSize="small" color="error" />
                    )}
                  </ListItemIcon>
                  <ListItemText
                    primary={extractFilename(entry.filepath)}
                    secondary={`${t('history.changesCount', { count: entry.changes.length })} — ${formatDate(entry.appliedAt)}`}
                  />
                </ListItem>
              ))}
            </List>
          </CardContent>
        </Card>
      )}
    </Box>
  );
}
