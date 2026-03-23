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
  RadarChart, PolarGrid, PolarAngleAxis, PolarRadiusAxis, Radar,
  ResponsiveContainer,
} from 'recharts';
import { getCollectionProfile } from '../../api/statsApi';
import type { CollectionProfile } from '../../types/types';
import KpiCard from './KpiCard';
import CamelotWheel from './CamelotWheel';

const COLORS = ['#00bcd4', '#7c4dff', '#ff5722', '#4caf50', '#ff9800', '#e91e63', '#2196f3', '#cddc39', '#9c27b0', '#795548', '#607d8b'];

export default function CollectionTab() {
  const [data, setData] = useState<CollectionProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const { t } = useTranslation();
  const theme = useTheme();
  const tickFill = theme.palette.text.primary;

  // Chargement du profil collection au montage du composant.
  // Appel unique ([] deps) : les données ne changent pas pendant la durée de vie du tab.
  useEffect(() => {
    getCollectionProfile()
      .then(setData)
      .catch(() => setData(null))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 2 }}>
        {Array.from({ length: 4 }, (_, i) => (
          <Skeleton key={i} variant="rectangular" height={250} sx={{ borderRadius: 1 }} />
        ))}
      </Box>
    );
  }

  if (!data || data.totalTracksScanned === 0) {
    return (
      <Typography color="text.secondary" sx={{ textAlign: 'center', mt: 4 }}>
        {t('stats.collection.empty')}
      </Typography>
    );
  }

  const completePct = data.totalTracksScanned > 0
    ? Math.round((data.totalWithCompleteTags / data.totalTracksScanned) * 100)
    : 0;

  // Genre donut — top 10 + Others
  const genreEntries = Object.entries(data.genreDistribution).sort((a, b) => b[1] - a[1]);
  const top10 = genreEntries.slice(0, 10);
  const othersCount = genreEntries.slice(10).reduce((s, [, v]) => s + v, 0);
  const genreData = [
    ...top10.map(([name, value]) => ({ name, value })),
    ...(othersCount > 0 ? [{ name: t('stats.collection.others'), value: othersCount }] : []),
  ];

  // BPM histogram
  const bpmData = Object.entries(data.bpmHistogram)
    .sort((a, b) => a[0].localeCompare(b[0], undefined, { numeric: true }))
    .map(([range, count]) => ({ range, count }));

  // Audio features radar
  const radarData = Object.entries(data.averageAudioFeatures).map(([feature, value]) => ({
    feature,
    value: Math.round(value * 100) / 100,
  }));

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
      {/* KPIs */}
      <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: 2 }}>
        <KpiCard value={data.totalTracksScanned} label={t('stats.collection.tracksScanned')} />
        <KpiCard value={data.totalTracksEnriched} label={t('stats.collection.tracksEnriched')} />
        <KpiCard value={`${completePct}%`} label={t('stats.collection.completeTags')} />
      </Box>

      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 2 }}>
        {/* Genre donut */}
        <Card variant="outlined">
          <CardContent>
            <Typography variant="subtitle2" gutterBottom>{t('stats.collection.genreDistribution')}</Typography>
            <ResponsiveContainer width="100%" height={280}>
              <PieChart>
                <Pie
                  data={genreData}
                  dataKey="value"
                  nameKey="name"
                  cx="50%"
                  cy="50%"
                  innerRadius={50}
                  outerRadius={100}
                  paddingAngle={2}
                >
                  {genreData.map((_, i) => (
                    <Cell key={i} fill={COLORS[i % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip />
                <Legend wrapperStyle={{ color: tickFill }} />
              </PieChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>

        {/* BPM histogram */}
        <Card variant="outlined">
          <CardContent>
            <Typography variant="subtitle2" gutterBottom>{t('stats.collection.bpmHistogram')}</Typography>
            <ResponsiveContainer width="100%" height={280}>
              <BarChart data={bpmData}>
                <XAxis dataKey="range" tick={{ fill: tickFill, fontSize: 11 }} />
                <YAxis tick={{ fill: tickFill }} allowDecimals={false} />
                <Tooltip />
                <Bar dataKey="count" fill={theme.palette.primary.main} radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>

        {/* Camelot Wheel */}
        <Card variant="outlined">
          <CardContent>
            <Typography variant="subtitle2" gutterBottom>{t('stats.collection.keyDistribution')}</Typography>
            <Box sx={{ display: 'flex', justifyContent: 'center' }}>
              <CamelotWheel keyDistribution={data.keyDistribution} />
            </Box>
          </CardContent>
        </Card>

        {/* Audio features radar */}
        {radarData.length > 0 && (
          <Card variant="outlined">
            <CardContent>
              <Typography variant="subtitle2" gutterBottom>{t('stats.collection.audioFeatures')}</Typography>
              <ResponsiveContainer width="100%" height={280}>
                <RadarChart data={radarData}>
                  <PolarGrid stroke={theme.palette.divider} />
                  <PolarAngleAxis dataKey="feature" tick={{ fill: tickFill, fontSize: 11 }} />
                  <PolarRadiusAxis tick={{ fill: tickFill, fontSize: 9 }} domain={[0, 1]} />
                  <Radar dataKey="value" stroke={theme.palette.secondary.main} fill={theme.palette.secondary.main} fillOpacity={0.3} />
                </RadarChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        )}
      </Box>
    </Box>
  );
}
