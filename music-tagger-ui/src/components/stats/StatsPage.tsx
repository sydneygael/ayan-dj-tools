import { useState } from 'react';
import Box from '@mui/material/Box';
import Tab from '@mui/material/Tab';
import Tabs from '@mui/material/Tabs';
import Typography from '@mui/material/Typography';
import { useTranslation } from 'react-i18next';
import CollectionTab from './CollectionTab';
import EnrichmentTab from './EnrichmentTab';
import ActivityTab from './ActivityTab';

export default function StatsPage() {
  const [tab, setTab] = useState(0);
  const { t } = useTranslation();

  return (
    <Box sx={{ maxWidth: 1000, mx: 'auto' }}>
      <Typography variant="h6" gutterBottom>
        {t('stats.title')}
      </Typography>

      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 3 }}>
        <Tab label={t('stats.tabs.collection')} />
        <Tab label={t('stats.tabs.enrichment')} />
        <Tab label={t('stats.tabs.activity')} />
      </Tabs>

      {tab === 0 && <CollectionTab />}
      {tab === 1 && <EnrichmentTab />}
      {tab === 2 && <ActivityTab />}
    </Box>
  );
}
