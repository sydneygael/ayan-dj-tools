import { render, screen } from '@testing-library/react';
import KpiCard from './KpiCard';

describe('KpiCard', () => {
  it('affiche la valeur et le label', () => {
    render(<KpiCard value={42} label="Total tracks" />);
    expect(screen.getByText('42')).toBeInTheDocument();
    expect(screen.getByText('Total tracks')).toBeInTheDocument();
  });

  it('affiche une valeur string', () => {
    render(<KpiCard value="85%" label="Match rate" />);
    expect(screen.getByText('85%')).toBeInTheDocument();
  });

  it('rend avec couleur personnalisée', () => {
    render(<KpiCard value={3} label="Errors" color="error.main" />);
    expect(screen.getByText('3')).toBeInTheDocument();
  });
});
