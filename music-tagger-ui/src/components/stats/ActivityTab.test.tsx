import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import ActivityTab from './ActivityTab';
import * as statsApi from '../../api/statsApi';
import type { ActivityTimeline, StatsReport } from '../../types/types';

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}));

vi.mock('@mui/material/styles', () => ({
  useTheme: () => ({
    palette: {
      primary: { main: '#00bcd4' },
      secondary: { main: '#7c4dff' },
      text: { primary: '#000' },
    },
  }),
}));

vi.mock('recharts', () => ({
  ResponsiveContainer: ({ children }: any) => <div>{children}</div>,
  AreaChart: ({ children }: any) => <div>{children}</div>,
  Area: () => null,
  BarChart: ({ children }: any) => <div>{children}</div>,
  Bar: () => null,
  XAxis: () => null,
  YAxis: () => null,
  Tooltip: () => null,
}));

vi.mock('../../utils/helpers', () => ({
  extractFilename: (p: string) => p.split('/').pop(),
  formatDate: (d: string) => d,
}));

const mockTimeline: ActivityTimeline = {
  plansPerPeriod: { '2026-03-20': 2 },
  tagsAppliedPerPeriod: { '2026-03-20': 5 },
  modeUsage: { PLAN: 2, MANUAL: 1 },
  averageDurationByMode: { PLAN: 10, MANUAL: 20 },
};

const mockStats: StatsReport = {
  totalPlansCreated: 3,
  totalTagsApplied: 10,
  totalFilesEnriched: 5,
  tagsAppliedByType: {},
  recentActivity: [],
};

describe('ActivityTab', () => {
  afterEach(() => vi.restoreAllMocks());

  beforeEach(() => {
    vi.spyOn(statsApi, 'getActivityTimeline').mockResolvedValue(mockTimeline);
    vi.spyOn(statsApi, 'getStats').mockResolvedValue(mockStats);
  });

  it('affiche les KPIs après chargement', async () => {
    render(<ActivityTab />);
    await waitFor(() => {
      // plansCount = sum of plansPerPeriod values = 2
      expect(screen.getByText('2')).toBeInTheDocument();
    });
  });

  it('re-fetch quand on change la période', async () => {
    render(<ActivityTab />);
    await waitFor(() => screen.getByText('2'));

    fireEvent.click(screen.getByText('stats.activity.week'));

    await waitFor(() => {
      expect(statsApi.getActivityTimeline).toHaveBeenCalledWith('week');
    });
  });

  it('affiche un message d erreur si fetch échoue', async () => {
    vi.spyOn(statsApi, 'getActivityTimeline').mockRejectedValue(new Error('network'));
    render(<ActivityTab />);
    await waitFor(() => {
      expect(screen.getByText('stats.loadError')).toBeInTheDocument();
    });
  });
});
