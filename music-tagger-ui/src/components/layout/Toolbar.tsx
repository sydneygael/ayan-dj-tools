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
import { useThemeStore } from '../../stores/themeStore';
import ModeSelector from '../mode/ModeSelector';

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

  return (
    <AppBar position="static" color="default" elevation={1} sx={{ borderBottom: 1, borderColor: 'divider' }}>
      <MuiToolbar variant="dense">
        {/* Logo + nom de l'app — cliquable pour revenir à l'accueil (chat) */}
        <IconButton edge="start" color="primary" onClick={() => navigate('/')} sx={{ mr: 1 }}>
          <LibraryMusicIcon />
        </IconButton>
        <Typography
          variant="subtitle1"
          fontWeight={600}
          sx={{ cursor: 'pointer' }}
          onClick={() => navigate('/')}
        >
          Ayan DJ Tools
        </Typography>

        {/* Spacer flexible pour pousser le reste à droite */}
        <Box sx={{ flex: 1 }} />

        {/* Sélecteur de mode d'opération global (PLAN / MANUAL / APPLY) */}
        <ModeSelector />

        {/* Icônes de navigation rapide + toggle thème */}
        <Box sx={{ ml: 2, display: 'flex', gap: 0.5 }}>
          <Tooltip title="Statistiques (Ctrl+S)">
            <IconButton onClick={() => navigate('/stats')}>
              <BarChartIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="Historique (Ctrl+H)">
            <IconButton onClick={() => navigate('/history')}>
              <HistoryIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="Parametres (Ctrl+,)">
            <IconButton onClick={() => navigate('/settings')}>
              <SettingsIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title={isDark ? 'Theme clair' : 'Theme sombre'}>
            <IconButton onClick={toggle}>
              {isDark ? <Brightness7Icon fontSize="small" /> : <Brightness4Icon fontSize="small" />}
            </IconButton>
          </Tooltip>
        </Box>
      </MuiToolbar>
    </AppBar>
  );
}
