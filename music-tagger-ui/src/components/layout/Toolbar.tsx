import AppBar from '@mui/material/AppBar';
import MuiToolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import Box from '@mui/material/Box';
import BarChartIcon from '@mui/icons-material/BarChart';
import HistoryIcon from '@mui/icons-material/History';
import SettingsIcon from '@mui/icons-material/Settings';
import Brightness4Icon from '@mui/icons-material/Brightness4';
import Brightness7Icon from '@mui/icons-material/Brightness7';
import LibraryMusicIcon from '@mui/icons-material/LibraryMusic';
import { useNavigate } from 'react-router';
import { useTranslation } from 'react-i18next';
import { useThemeStore } from '../../stores/themeStore';
import ModeSelector from '../mode/ModeSelector';
import BackendStatusChip from '../common/BackendStatusChip';

/**
 * Barre de navigation supérieure.
 * Contient le logo/lien accueil, le sélecteur de mode (PLAN/MANUAL/APPLY),
 * les icônes de navigation (stats, historique, paramètres) et le toggle thème sombre/clair.
 * Les tooltips affichent aussi les raccourcis clavier associés.
 */
export default function Toolbar() {
  const navigate = useNavigate();
  const isDark = useThemeStore((s) => s.isDark);
  const toggle = useThemeStore((s) => s.toggle);
  const { t } = useTranslation();

  return (
    <AppBar position="static" color="default" elevation={1} sx={{ borderBottom: 1, borderColor: 'divider' }}>
      <MuiToolbar variant="dense">
        {/* Logo + nom de l'app — cliquable pour revenir à l'accueil (chat) */}
        <IconButton edge="start" color="primary" onClick={() => navigate('/')} sx={{ mr: 1 }} aria-label={t('toolbar.home')}>
          <LibraryMusicIcon />
        </IconButton>
        <Typography
          variant="subtitle1"
          fontWeight={600}
          sx={{ cursor: 'pointer' }}
          onClick={() => navigate('/')}
        >
          {t('toolbar.brand')}
        </Typography>

        {/* Spacer flexible pour pousser le reste à droite */}
        <Box sx={{ flex: 1 }} />

        {/* Sélecteur de mode d'opération global (PLAN / MANUAL / APPLY) */}
        <ModeSelector />

        {/* Icônes de navigation rapide + toggle thème */}
        <Box sx={{ ml: 2, display: 'flex', gap: 0.5, alignItems: 'center' }}>
          <BackendStatusChip />
          <Tooltip title={t('toolbar.stats')}>
            <IconButton onClick={() => navigate('/stats')} aria-label={t('toolbar.statsLabel')}>
              <BarChartIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title={t('toolbar.history')}>
            <IconButton onClick={() => navigate('/history')} aria-label={t('toolbar.historyLabel')}>
              <HistoryIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title={t('toolbar.settings')}>
            <IconButton onClick={() => navigate('/settings')} aria-label={t('toolbar.settingsLabel')}>
              <SettingsIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title={isDark ? t('toolbar.lightTheme') : t('toolbar.darkTheme')}>
            <IconButton onClick={toggle} aria-label={t('toolbar.toggleTheme')}>
              {isDark ? <Brightness7Icon fontSize="small" /> : <Brightness4Icon fontSize="small" />}
            </IconButton>
          </Tooltip>
        </Box>
      </MuiToolbar>
    </AppBar>
  );
}
