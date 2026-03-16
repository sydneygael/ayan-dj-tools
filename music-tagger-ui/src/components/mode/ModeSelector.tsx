import ToggleButton from '@mui/material/ToggleButton';
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup';
import { useTranslation } from 'react-i18next';
import { useModeStore } from '../../stores/modeStore';
import type { OperatingMode } from '../../types/types';

/**
 * Sélecteur du mode d'opération global (PLAN / MANUAL / APPLY).
 * Utilise le store Zustand `modeStore` pour lire et modifier le mode.
 * Le mode est persisté dans localStorage via le store.
 * - PLAN : revue par lot avant application
 * - MANUAL : confirmation fichier par fichier
 * - APPLY : application automatique sans confirmation
 */
export default function ModeSelector() {
  const mode = useModeStore((s) => s.mode);
  const setMode = useModeStore((s) => s.setMode);
  const { t } = useTranslation();

  return (
    <ToggleButtonGroup
      value={mode}
      exclusive
      onChange={(_, v) => v && setMode(v as OperatingMode)}
      size="small"
    >
      <ToggleButton value="PLAN">{t('mode.plan')}</ToggleButton>
      <ToggleButton value="MANUAL">{t('mode.manual')}</ToggleButton>
      <ToggleButton value="APPLY">{t('mode.apply')}</ToggleButton>
    </ToggleButtonGroup>
  );
}
