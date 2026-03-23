import { useEffect, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Skeleton from '@mui/material/Skeleton';
import Typography from '@mui/material/Typography';
import { useTheme } from '@mui/material/styles';
import { useTranslation } from 'react-i18next';
import {
  PieChart, Pie, Cell, BarChart, Bar, XAxis, YAxis, Tooltip, Legend,
  ResponsiveContainer,
} from 'recharts';
import { getEnrichmentStats } from '../../api/statsApi';
import type { EnrichmentStats } from '../../types/types';
import KpiCard from './KpiCard';

const SOURCE_COLORS: Record<string, string> = { Spotify: '#1DB954' };
const FALLBACK_COLORS = ['#7c4dff', '#ff5722', '#00bcd4', '#ff9800', '#e91e63'];

export default function EnrichmentTab() {
  const [data, setData] = useState<EnrichmentStats | null>(null);
  const [loading, setLoading] = useState(true);
  const { t } = useTranslation();
  const theme = useTheme();
  const tickFill = theme.palette.text.primary;

  // Chargement des statistiques d'enrichissement au montage.
  // Appel unique ([] deps) : les données sont statiques le temps que le tab est affiché.
  useEffect(() => {
    getEnrichmentStats()
      .then(setData)
      .catch(() => setData(null))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 2 }}>
        {Array.from({ length: 3 }, (_, i) => (
          <Skeleton key={i} variant="rectangular" height={250} sx={{ borderRadius: 1 }} />
        ))}
      </Box>
    );
  }

  if (!data) {
    return (
      <Typography color="text.secondary" sx={{ textAlign: 'center', mt: 4 }}>
        {t('stats.loadError')}
      </Typography>
    );
  }

  const matchPct = Math.round(data.spotifyMatchRate * 100);
  const errorPct = Math.round(data.errorRate * 100);
  const totalEnriched = Object.values(data.mostEnrichedTagTypes).reduce((s, v) => s + v, 0);

  // Tag types — vertical bar chart, sorted desc
  const tagTypeData = Object.entries(data.mostEnrichedTagTypes)
    .sort((a, b) => b[1] - a[1])
    .map(([name, value]) => ({ name, value }));

  // Source pie
  const sourceData = Object.entries(data.enrichmentBySource).map(([name, value]) => ({ name, value }));
  let fallbackIdx = 0;

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
      {/* KPIs */}
      <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: 2 }}>
        <KpiCard value={`${matchPct}%`} label={t('stats.enrichment.matchRate')} color="success.main" />
        <KpiCard value={`${errorPct}%`} label={t('stats.enrichment.errorRate')} color="error.main" />
        <KpiCard value={totalEnriched} label={t('stats.enrichment.totalEnriched')} />
      </Box>

      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 2 }}>
        {/* Tag types bar chart */}
        {tagTypeData.length > 0 && (
          <Card variant="outlined">
            <CardContent>
              <Typography variant="subtitle2" gutterBottom>{t('stats.enrichment.tagTypes')}</Typography>
              <ResponsiveContainer width="100%" height={280}>
                <BarChart data={tagTypeData} layout="vertical">
                  <XAxis type="number" tick={{ fill: tickFill }} allowDecimals={false} />
                  <YAxis type="category" dataKey="name" tick={{ fill: tickFill, fontSize: 11 }} width={80} />
                  <Tooltip />
                  <Bar dataKey="value" fill={theme.palette.primary.main} radius={[0, 4, 4, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        )}

        {/* Source pie */}
        {sourceData.length > 0 && (
          <Card variant="outlined">
            <CardContent>
              <Typography variant="subtitle2" gutterBottom>{t('stats.enrichment.bySource')}</Typography>
              <ResponsiveContainer width="100%" height={280}>
                <PieChart>
                  <Pie
                    data={sourceData}
                    dataKey="value"
                    nameKey="name"
                    cx="50%"
                    cy="50%"
                    outerRadius={100}
                    paddingAngle={2}
                  >
                    {sourceData.map((entry) => {
                      const color = SOURCE_COLORS[entry.name] ?? FALLBACK_COLORS[fallbackIdx++ % FALLBACK_COLORS.length];
                      return <Cell key={entry.name} fill={color} />;
                    })}
                  </Pie>
                  <Tooltip />
                  <Legend wrapperStyle={{ color: tickFill }} />
                </PieChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        )}
      </Box>
    </Box>
  );
}
