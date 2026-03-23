import { render, screen } from '@testing-library/react';
import CamelotWheel from './CamelotWheel';

vi.mock('@mui/material/styles', () => ({
  useTheme: () => ({
    palette: {
      primary: { main: '#00bcd4' },
      text: { primary: '#000' },
      divider: '#ccc',
    },
  }),
}));

describe('CamelotWheel', () => {
  it('affiche le message vide quand keyDistribution est vide', () => {
    render(<CamelotWheel keyDistribution={{}} />);
    expect(screen.getByText('No key data available')).toBeInTheDocument();
  });

  it('rend un SVG avec des données', () => {
    const { container } = render(<CamelotWheel keyDistribution={{ Am: 3, C: 2, G: 1 }} />);
    const svg = container.querySelector('svg');
    expect(svg).toBeInTheDocument();
  });

  it('affiche les labels Camelot dans le SVG', () => {
    render(<CamelotWheel keyDistribution={{ C: 2 }} />);
    // C → 8B (major ring)
    expect(screen.getByText('8B')).toBeInTheDocument();
    // 1B doit aussi être présent (tous les segments sont rendus)
    expect(screen.getByText('1B')).toBeInTheDocument();
  });
});
