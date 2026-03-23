import { render, screen, waitFor } from '@testing-library/react';
import CollectionTab from './CollectionTab';
import * as statsApi from '../../api/statsApi';
import type { CollectionProfile } from '../../types/types';

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}));

vi.mock('@mui/material/styles', () => ({
  useTheme: () => ({
    palette: {
      primary: { main: '#00bcd4' },
      secondary: { main: '#7c4dff' },
      text: { primary: '#000' },
      divider: '#ccc',
    },
  }),
}));

vi.mock('recharts', () => ({
  ResponsiveContainer: ({ children }: any) => <div>{children}</div>,
  PieChart: ({ children }: any) => <div data-testid="pie-chart">{children}</div>,
  Pie: () => null,
  Cell: () => null,
  BarChart: ({ children }: any) => <div data-testid="bar-chart">{children}</div>,
  Bar: () => null,
  XAxis: () => null,
  YAxis: () => null,
  Tooltip: () => null,
  Legend: () => null,
  RadarChart: ({ children }: any) => <div>{children}</div>,
  PolarGrid: () => null,
  PolarAngleAxis: () => null,
  PolarRadiusAxis: () => null,
  Radar: () => null,
}));

vi.mock('./CamelotWheel', () => ({ default: () => <div data-testid="camelot-wheel" /> }));

const emptyProfile: CollectionProfile = {
  genreDistribution: {},
  bpmHistogram: {},
  keyDistribution: {},
  averageAudioFeatures: {},
  totalTracksScanned: 0,
  totalTracksEnriched: 0,
  totalWithCompleteTags: 0,
};

const fullProfile: CollectionProfile = {
  genreDistribution: { Techno: 3, House: 2 },
  bpmHistogram: { '125-130': 2 },
  keyDistribution: { Am: 3 },
  averageAudioFeatures: { energy: 0.8, danceability: 0.7 },
  totalTracksScanned: 5,
  totalTracksEnriched: 5,
  totalWithCompleteTags: 3,
};

describe('CollectionTab', () => {
  afterEach(() => vi.restoreAllMocks());

  it('affiche les KPIs après chargement', async () => {
    vi.spyOn(statsApi, 'getCollectionProfile').mockResolvedValue(fullProfile);
    render(<CollectionTab />);
    await waitFor(() => {
      expect(screen.getByText('stats.collection.tracksScanned')).toBeInTheDocument();
      expect(screen.getByText('60%')).toBeInTheDocument(); // totalWithCompleteTags / totalTracksScanned
    });
  });

  it('affiche le message vide quand totalTracksScanned = 0', async () => {
    vi.spyOn(statsApi, 'getCollectionProfile').mockResolvedValue(emptyProfile);
    render(<CollectionTab />);
    await waitFor(() => {
      expect(screen.getByText('stats.collection.empty')).toBeInTheDocument();
    });
  });
});
