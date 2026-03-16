import { useRef, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';
import TextField from '@mui/material/TextField';
import Switch from '@mui/material/Switch';
import Button from '@mui/material/Button';
import FormControlLabel from '@mui/material/FormControlLabel';
import ToggleButton from '@mui/material/ToggleButton';
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup';
import DownloadIcon from '@mui/icons-material/Download';
import UploadIcon from '@mui/icons-material/Upload';
import { useTranslation } from 'react-i18next';
import { environment } from '../../config/environment';
import { useModeStore } from '../../stores/modeStore';
import { useThemeStore } from '../../stores/themeStore';
import { useLanguageStore } from '../../stores/languageStore';
import { useNotification } from '../../utils/notifications';
import type { OperatingMode } from '../../types/types';

/**
 * Page de paramètres de l'application (route /settings).
 * Quatre sections :
 * - Connexion : URL API (lecture seule) + toggle WebSocket (persisté en localStorage)
 * - Préférences : mode par défaut (PLAN/MANUAL/APPLY) + thème sombre/clair
 * - Langue : sélecteur FR/EN
 * - Export/Import : sauvegarde et restauration des paramètres au format JSON
 */
export default function SettingsPage() {
  const mode = useModeStore((s) => s.mode);
  const setMode = useModeStore((s) => s.setMode);
  const isDark = useThemeStore((s) => s.isDark);
  const toggle = useThemeStore((s) => s.toggle);
  const language = useLanguageStore((s) => s.language);
  const setLanguage = useLanguageStore((s) => s.setLanguage);
  const notify = useNotification();
  const { t } = useTranslation();
  // Initialisation lazy : lit la valeur depuis localStorage au premier rendu uniquement
  const [wsEnabled, setWsEnabled] = useState(() => localStorage.getItem('wsEnabled') !== 'false');
  const fileInput = useRef<HTMLInputElement>(null);

  /** Persiste le choix WebSocket dans localStorage et met à jour l'état local. */
  const saveWs = (enabled: boolean) => {
    setWsEnabled(enabled);
    localStorage.setItem('wsEnabled', String(enabled));
  };

  /** Exporte les paramètres actuels dans un fichier JSON téléchargé par le navigateur. */
  const exportSettings = () => {
    const data = {
      apiUrl: environment.apiUrl,
      defaultMode: mode,
      darkTheme: isDark,
      wsEnabled,
      language,
    };
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'ayan-settings.json';
    a.click();
    URL.revokeObjectURL(url);
    notify.success(t('settings.exported'));
  };

  /** Importe les paramètres depuis un fichier JSON sélectionné par l'utilisateur. */
  const importSettings = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      try {
        const data = JSON.parse(reader.result as string);
        if (data.defaultMode) setMode(data.defaultMode);
        if (typeof data.darkTheme === 'boolean' && data.darkTheme !== isDark) toggle();
        if (typeof data.wsEnabled === 'boolean') saveWs(data.wsEnabled);
        if (data.language === 'fr' || data.language === 'en') setLanguage(data.language);
        notify.success(t('settings.imported'));
      } catch {
        notify.error(t('settings.invalidFile'));
      }
    };
    reader.readAsText(file);
  };

  return (
    <Box sx={{ maxWidth: 600, mx: 'auto', display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Typography variant="h6">{t('settings.title')}</Typography>

      <Card variant="outlined">
        <CardContent>
          <Typography variant="subtitle2" gutterBottom>
            {t('settings.connection')}
          </Typography>
          <TextField
            fullWidth
            size="small"
            label={t('settings.apiUrl')}
            value={environment.apiUrl}
            slotProps={{ input: { readOnly: true } }}
            sx={{ mb: 1 }}
          />
          <FormControlLabel
            control={<Switch checked={wsEnabled} onChange={(_, v) => saveWs(v)} />}
            label={t('settings.wsEnabled')}
          />
        </CardContent>
      </Card>

      <Card variant="outlined">
        <CardContent>
          <Typography variant="subtitle2" gutterBottom>
            {t('settings.preferences')}
          </Typography>
          <Typography variant="body2" color="text.secondary" gutterBottom>
            {t('settings.defaultMode')}
          </Typography>
          <ToggleButtonGroup
            value={mode}
            exclusive
            onChange={(_, v) => v && setMode(v as OperatingMode)}
            size="small"
            sx={{ mb: 1 }}
          >
            <ToggleButton value="PLAN">{t('mode.plan')}</ToggleButton>
            <ToggleButton value="MANUAL">{t('mode.manual')}</ToggleButton>
            <ToggleButton value="APPLY">{t('mode.apply')}</ToggleButton>
          </ToggleButtonGroup>
          <Box>
            <FormControlLabel
              control={<Switch checked={isDark} onChange={toggle} />}
              label={t('settings.darkTheme')}
            />
          </Box>
        </CardContent>
      </Card>

      <Card variant="outlined">
        <CardContent>
          <Typography variant="subtitle2" gutterBottom>
            {t('settings.language')}
          </Typography>
          <ToggleButtonGroup
            value={language}
            exclusive
            onChange={(_, v) => v && setLanguage(v as 'fr' | 'en')}
            size="small"
          >
            <ToggleButton value="fr">{t('settings.languageFr')}</ToggleButton>
            <ToggleButton value="en">{t('settings.languageEn')}</ToggleButton>
          </ToggleButtonGroup>
        </CardContent>
      </Card>

      <Card variant="outlined">
        <CardContent>
          <Typography variant="subtitle2" gutterBottom>
            {t('settings.exportImport')}
          </Typography>
          <Box sx={{ display: 'flex', gap: 1 }}>
            <Button variant="outlined" startIcon={<DownloadIcon />} onClick={exportSettings}>
              {t('settings.export')}
            </Button>
            <Button variant="outlined" startIcon={<UploadIcon />} onClick={() => fileInput.current?.click()}>
              {t('settings.import')}
            </Button>
            <input ref={fileInput} type="file" accept=".json" hidden onChange={importSettings} />
          </Box>
        </CardContent>
      </Card>
    </Box>
  );
}
