import { lazy, Suspense, useMemo, useState } from 'react';
import { BrowserRouter, Route, Routes } from 'react-router';
import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import CircularProgress from '@mui/material/CircularProgress';
import Box from '@mui/material/Box';
import { SnackbarProvider } from 'notistack';
import { useThemeStore } from './stores/themeStore';
import { darkTheme, lightTheme } from './theme/theme';
import { useKeyboardShortcuts } from './hooks/useKeyboardShortcuts';
import AppLayout from './components/layout/AppLayout';
import ChatPage from './components/chat/ChatPage';
import ShortcutsHelpDialog from './components/dialogs/ShortcutsHelpDialog';

// Chargement paresseux (lazy) des pages secondaires pour réduire le bundle initial.
// Seule ChatPage (page d'accueil) est chargée immédiatement.
const PlanReviewPage = lazy(() => import('./components/plan/PlanReviewPage'));
const HistoryPage = lazy(() => import('./components/history/HistoryPage'));
const SettingsPage = lazy(() => import('./components/settings/SettingsPage'));
const StatsPage = lazy(() => import('./components/stats/StatsPage'));

/** Spinner de chargement affiché pendant le lazy-loading des pages. */
const Loading = () => (
  <Box sx={{ display: 'flex', justifyContent: 'center', mt: 8 }}>
    <CircularProgress />
  </Box>
);

/**
 * Contenu principal de l'application, encapsulé dans un composant séparé
 * pour que useKeyboardShortcuts ait accès au contexte du Router (useNavigate).
 * Gère le routage et la dialog d'aide raccourcis.
 */
function AppContent() {
  const [helpOpen, setHelpOpen] = useState(false);
  // Active les raccourcis clavier globaux ; le callback ouvre la dialog d'aide (touche ?)
  useKeyboardShortcuts(() => setHelpOpen(true));

  return (
    <>
      {/* Toutes les routes sont imbriquées sous AppLayout (toolbar + sidebar + outlet) */}
      <Routes>
        <Route element={<AppLayout />}>
          {/* Route index = page de chat (accueil) */}
          <Route index element={<ChatPage />} />
          {/* Pages lazy-loadées avec Suspense et spinner de fallback */}
          <Route
            path="plan/:id"
            element={
              <Suspense fallback={<Loading />}>
                <PlanReviewPage />
              </Suspense>
            }
          />
          <Route
            path="history"
            element={
              <Suspense fallback={<Loading />}>
                <HistoryPage />
              </Suspense>
            }
          />
          <Route
            path="settings"
            element={
              <Suspense fallback={<Loading />}>
                <SettingsPage />
              </Suspense>
            }
          />
          <Route
            path="stats"
            element={
              <Suspense fallback={<Loading />}>
                <StatsPage />
              </Suspense>
            }
          />
        </Route>
      </Routes>
      {/* Dialog d'aide raccourcis clavier, ouverte via la touche ? */}
      <ShortcutsHelpDialog open={helpOpen} onClose={() => setHelpOpen(false)} />
    </>
  );
}

/**
 * Composant racine de l'application.
 * Fournit le thème MUI (sombre/clair), le reset CSS, les notifications (Snackbar)
 * et le routeur. Le thème est mémorisé pour éviter les re-renders inutiles.
 */
export default function App() {
  const isDark = useThemeStore((s) => s.isDark);
  // Mémoisation du thème : ne recalcule que quand isDark change
  const theme = useMemo(() => (isDark ? darkTheme : lightTheme), [isDark]);

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <SnackbarProvider maxSnack={3} anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}>
        <BrowserRouter>
          <AppContent />
        </BrowserRouter>
      </SnackbarProvider>
    </ThemeProvider>
  );
}
