import Box from '@mui/material/Box';
import Drawer from '@mui/material/Drawer';
import { Outlet, useLocation } from 'react-router';
import { useTranslation } from 'react-i18next';
import Toolbar from './Toolbar';
import Sidebar from './Sidebar';

/** Largeur fixe du tiroir latéral (sidebar) en pixels. */
const DRAWER_WIDTH = 280;

/**
 * Layout principal de l'application.
 * Structure : Toolbar (haut) + Sidebar permanente (gauche) + zone de contenu (Outlet).
 * Le `key={location.pathname}` sur la zone principale force le re-mount à chaque
 * changement de route, déclenchant l'animation CSS "page-enter" (fadeIn).
 */
export default function AppLayout() {
  const location = useLocation();
  const { t } = useTranslation();

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
      <Toolbar />
      <Box sx={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
        {/* Sidebar permanente : toujours visible, pas de toggle */}
        <Drawer
          variant="permanent"
          aria-label={t('sidebar.sidebarLabel')}
          sx={{
            width: DRAWER_WIDTH,
            flexShrink: 0,
            '& .MuiDrawer-paper': {
              width: DRAWER_WIDTH,
              position: 'relative',
              borderRight: 1,
              borderColor: 'divider',
            },
          }}
        >
          <Sidebar />
        </Drawer>
        {/* Zone de contenu principal : affiche le composant de la route active */}
        <Box
          key={location.pathname}
          component="main"
          className="page-enter"
          sx={{ flex: 1, overflow: 'auto', p: 2 }}
        >
          <Outlet />
        </Box>
      </Box>
    </Box>
  );
}
