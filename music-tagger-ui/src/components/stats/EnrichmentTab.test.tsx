import { render, screen, waitFor } from '@testing-library/react';
import EnrichmentTab from './EnrichmentTab';
import * as statsApi from '../../api/statsApi';
import type { EnrichmentStats } from '../../types/types';

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}));

vi.mock('@mui/material/styles', () => ({
  useTheme: () => ({
    palette: {
      primary: { main: '#00bcd4' },
      text: { primary: '#000' },
    },
  }),
}));

vi.mock('recharts', () => ({
  ResponsiveContainer: ({ children }: any) => <div>{children}</div>,
  PieChart: ({ children }: any) => <div>{children}</div>,
  Pie: () => null,
  Cell: () => null,
  BarChart: ({ children }: any) => <div>{children}</div>,
  Bar: () => null,
  XAxis: () => null,
  YAxis: () => null,
  Tooltip: () => null,
  Legend: () => null,
}));

const mockStats: EnrichmentStats = {
  spotifyMatchRate: 0.75,  // 0-1, le composant multiplie par 100
  errorRate: 0.25,
  mostEnrichedTagTypes: { genre: 10, bpm: 8 },
  enrichmentBySource: { spotify: 10 },
};

describe('EnrichmentTab', () => {
  afterEach(() => vi.restoreAllMocks());

  it('affiche les KPIs après chargement', async () => {
    vi.spyOn(statsApi, 'getEnrichmentStats').mockResolvedValue(mockStats);
    render(<EnrichmentTab />);
    await waitFor(() => {
      expect(screen.getByText('75%')).toBeInTheDocument(); // matchRate
      expect(screen.getByText('25%')).toBeInTheDocument(); // errorRate
    });
  });

  it('affiche un message d erreur si fetch échoue', async () => {
    vi.spyOn(statsApi, 'getEnrichmentStats').mockRejectedValue(new Error('network'));
    render(<EnrichmentTab />);
    await waitFor(() => {
      expect(screen.getByText('stats.loadError')).toBeInTheDocument();
    });
  });
});
