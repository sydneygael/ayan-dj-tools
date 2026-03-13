import { createTheme } from '@mui/material/styles';

/**
 * Thème sombre (défaut) — palette cyan (#00bcd4) / violet (#7c4dff).
 * Fond sombre (#121212) avec cartes légèrement plus claires (#1e1e1e).
 * Tous les composants interactifs sont en taille "small" par défaut (densité -1).
 */
export const darkTheme = createTheme({
  palette: {
    mode: 'dark',
    primary: { main: '#00bcd4' },    // Cyan — couleur principale (boutons, liens, accents)
    secondary: { main: '#7c4dff' },  // Violet — couleur secondaire (badges, highlights)
    background: {
      default: '#121212',             // Fond global de l'application
      paper: '#1e1e1e',              // Fond des cartes, dialogs, drawers
    },
  },
  typography: {
    fontFamily: 'Roboto, sans-serif',
  },
  components: {
    MuiButton: {
      defaultProps: { size: 'small' },
    },
    MuiTextField: {
      defaultProps: { size: 'small' },
    },
    MuiIconButton: {
      defaultProps: { size: 'small' },
    },
  },
});

/**
 * Thème clair — palette teal foncé (#00838f) / violet profond (#651fff).
 * Fond clair (#fafafa) avec cartes blanches (#ffffff).
 * Mêmes composants par défaut en taille "small".
 */
export const lightTheme = createTheme({
  palette: {
    mode: 'light',
    primary: { main: '#00838f' },    // Teal foncé — lisible sur fond clair
    secondary: { main: '#651fff' },  // Violet profond
    background: {
      default: '#fafafa',
      paper: '#ffffff',
    },
  },
  typography: {
    fontFamily: 'Roboto, sans-serif',
  },
  components: {
    MuiButton: {
      defaultProps: { size: 'small' },
    },
    MuiTextField: {
      defaultProps: { size: 'small' },
    },
    MuiIconButton: {
      defaultProps: { size: 'small' },
    },
  },
});
