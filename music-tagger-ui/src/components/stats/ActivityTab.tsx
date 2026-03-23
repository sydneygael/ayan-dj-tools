import { useEffect, useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Skeleton from '@mui/material/Skeleton';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TablePagination from '@mui/material/TablePagination';
import TableRow from '@mui/material/TableRow';
import ToggleButton from '@mui/material/ToggleButton';
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup';
import Typography from '@mui/material/Typography';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ErrorIcon from '@mui/icons-material/Error';
import { useTheme } from '@mui/material/styles';
import { useTranslation } from 'react-i18next';
import {
  AreaChart, Area, BarChart, Bar, XAxis, YAxis, Tooltip,
  ResponsiveContainer,
} from 'recharts';
import { getActivityTimeline, getStats } from '../../api/statsApi';
import type { ActivityTimeline, StatsReport } from '../../types/types';
import { extractFilename, formatDate } from '../../utils/helpers';
import KpiCard from './KpiCard';

type Period = 'week' | 'month' | 'all';

export default function ActivityTab() {
  const [timeline, setTimeline] = useState<ActivityTimeline | null>(null);
  const [stats, setStats] = useState<StatsReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [period, setPeriod] = useState<Period>('month');
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(5);
  const { t } = useTranslation();
  const theme = useTheme();
  const tickFill = theme.palette.text.primary;

  // Chargement des données d'activité à chaque changement de période (week/month/all).
  // Récupère en parallèle la timeline (pour les charts) et les stats générales (pour le tableau récent).
  // Se re-déclenche quand l'utilisateur change le ToggleButtonGroup de période.
  useEffect(() => {
    setLoading(true);
    Promise.all([getActivityTimeline(period), getStats()])
      .then(([tl, st]) => { setTimeline(tl); setStats(st); })
      .catch(() => { setTimeline(null); setStats(null); })
      .finally(() => setLoading(false));
  }, [period]);

  // Transforme la map {date → count} en tableau trié chronologiquement pour l'AreaChart.
  // Recalculé uniquement quand timeline change (après un fetch).
  const tagsAreaData = useMemo(() => {
    if (!timeline) return [];
    return Object.entries(timeline.tagsAppliedPerPeriod)
      .sort((a, b) => a[0].localeCompare(b[0]))
      .map(([date, count]) => ({ date, count }));
  }, [timeline]);

  // Transforme la map {mode → count} en tableau pour le BarChart horizontal.
  const modeBarData = useMemo(() => {
    if (!timeline) return [];
    return Object.entries(timeline.modeUsage).map(([mode, count]) => ({ mode, count }));
  }, [timeline]);

  // Somme des plans créés sur la période sélectionnée (KPI card).
  const plansCount = useMemo(() => {
    if (!timeline) return 0;
    return Object.values(timeline.plansPerPeriod).reduce((s, v) => s + v, 0);
  }, [timeline]);

  // Durée moyenne d'exécution tous modes confondus. Affiche "--" si aucune donnée.
  const avgDuration = useMemo(() => {
    if (!timeline) return '--';
    const entries = Object.values(timeline.averageDurationByMode);
    if (entries.length === 0) return '--';
    const avg = entries.reduce((s, v) => s + v, 0) / entries.length;
    return `${Math.round(avg)}s`;
  }, [timeline]);

  if (loading) {
    return (
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
        <Skeleton variant="rectangular" height={40} width={200} sx={{ borderRadius: 1 }} />
        <Skeleton variant="rectangular" height={250} sx={{ borderRadius: 1 }} />
      </Box>
    );
  }

  if (!timeline) {
    return (
      <Typography color="text.secondary" sx={{ textAlign: 'center', mt: 4 }}>
        {t('stats.loadError')}
      </Typography>
    );
  }

  const activity = stats?.recentActivity ?? [];
  const paginatedActivity = activity.slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage);

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
      {/* Period toggle */}
      <ToggleButtonGroup
        value={period}
        exclusive
        onChange={(_, v) => v && setPeriod(v as Period)}
        size="small"
      >
        <ToggleButton value="week">{t('stats.activity.week')}</ToggleButton>
        <ToggleButton value="month">{t('stats.activity.month')}</ToggleButton>
        <ToggleButton value="all">{t('stats.activity.all')}</ToggleButton>
      </ToggleButtonGroup>

      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 2 }}>
        {/* Tags area chart */}
        {tagsAreaData.length > 0 && (
          <Card variant="outlined">
            <CardContent>
              <Typography variant="subtitle2" gutterBottom>{t('stats.activity.tagsApplied')}</Typography>
              <ResponsiveContainer width="100%" height={250}>
                <AreaChart data={tagsAreaData}>
                  <XAxis dataKey="date" tick={{ fill: tickFill, fontSize: 10 }} />
                  <YAxis tick={{ fill: tickFill }} allowDecimals={false} />
                  <Tooltip />
                  <Area
                    type="monotone"
                    dataKey="count"
                    stroke={theme.palette.primary.main}
                    fill={theme.palette.primary.main}
                    fillOpacity={0.2}
                  />
                </AreaChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        )}

        {/* Mode usage bar */}
        {modeBarData.length > 0 && (
          <Card variant="outlined">
            <CardContent>
              <Typography variant="subtitle2" gutterBottom>{t('stats.activity.modeUsage')}</Typography>
              <ResponsiveContainer width="100%" height={250}>
                <BarChart data={modeBarData} layout="vertical">
                  <XAxis type="number" tick={{ fill: tickFill }} allowDecimals={false} />
                  <YAxis type="category" dataKey="mode" tick={{ fill: tickFill }} width={70} />
                  <Tooltip />
                  <Bar dataKey="count" fill={theme.palette.secondary.main} radius={[0, 4, 4, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        )}
      </Box>

      {/* KPIs */}
      <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: 2 }}>
        <KpiCard value={plansCount} label={t('stats.activity.plansCount')} />
        <KpiCard value={avgDuration} label={t('stats.activity.avgDuration')} />
      </Box>

      {/* Recent activity table */}
      {activity.length > 0 && (
        <Card variant="outlined">
          <CardContent>
            <Typography variant="subtitle2" gutterBottom>{t('stats.recentActivity')}</Typography>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>{t('history.status')}</TableCell>
                  <TableCell>{t('history.file')}</TableCell>
                  <TableCell>{t('history.changes')}</TableCell>
                  <TableCell>{t('history.date')}</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {paginatedActivity.map((entry, i) => (
                  <TableRow key={i}>
                    <TableCell>
                      {entry.success
                        ? <CheckCircleIcon fontSize="small" color="success" />
                        : <ErrorIcon fontSize="small" color="error" />}
                    </TableCell>
                    <TableCell>{extractFilename(entry.filepath)}</TableCell>
                    <TableCell>{entry.changes.length}</TableCell>
                    <TableCell>{formatDate(entry.appliedAt)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            <TablePagination
              component="div"
              count={activity.length}
              page={page}
              onPageChange={(_, p) => setPage(p)}
              rowsPerPage={rowsPerPage}
              onRowsPerPageChange={(e) => { setRowsPerPage(parseInt(e.target.value, 10)); setPage(0); }}
              rowsPerPageOptions={[5, 10, 25]}
            />
          </CardContent>
        </Card>
      )}
    </Box>
  );
}
