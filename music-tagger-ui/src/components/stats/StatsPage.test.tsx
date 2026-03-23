import { render, screen, fireEvent } from '@testing-library/react';
import StatsPage from './StatsPage';

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}));

vi.mock('./CollectionTab', () => ({ default: () => <div data-testid="collection-tab" /> }));
vi.mock('./EnrichmentTab', () => ({ default: () => <div data-testid="enrichment-tab" /> }));
vi.mock('./ActivityTab', () => ({ default: () => <div data-testid="activity-tab" /> }));

describe('StatsPage', () => {
  it('affiche les 3 onglets', () => {
    render(<StatsPage />);
    expect(screen.getByText('stats.tabs.collection')).toBeInTheDocument();
    expect(screen.getByText('stats.tabs.enrichment')).toBeInTheDocument();
    expect(screen.getByText('stats.tabs.activity')).toBeInTheDocument();
  });

  it('affiche CollectionTab par défaut', () => {
    render(<StatsPage />);
    expect(screen.getByTestId('collection-tab')).toBeInTheDocument();
    expect(screen.queryByTestId('enrichment-tab')).not.toBeInTheDocument();
  });

  it('affiche EnrichmentTab après clic sur onglet 1', () => {
    render(<StatsPage />);
    fireEvent.click(screen.getByText('stats.tabs.enrichment'));
    expect(screen.getByTestId('enrichment-tab')).toBeInTheDocument();
    expect(screen.queryByTestId('collection-tab')).not.toBeInTheDocument();
  });

  it('affiche ActivityTab après clic sur onglet 2', () => {
    render(<StatsPage />);
    fireEvent.click(screen.getByText('stats.tabs.activity'));
    expect(screen.getByTestId('activity-tab')).toBeInTheDocument();
  });
});
