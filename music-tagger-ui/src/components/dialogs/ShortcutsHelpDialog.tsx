import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableRow from '@mui/material/TableRow';
import DialogActions from '@mui/material/DialogActions';
import Button from '@mui/material/Button';
import { useTranslation } from 'react-i18next';

interface Props {
  open: boolean;
  onClose: () => void;
}

/**
 * Dialog d'aide affichant tous les raccourcis clavier.
 * Ouverte via la touche "?" ou depuis le hook useKeyboardShortcuts.
 * Présente un tableau simple clé → action.
 */
export default function ShortcutsHelpDialog({ open, onClose }: Props) {
  const { t } = useTranslation();

  const shortcuts = [
    { keys: 'Ctrl+P', action: t('dialogs.modePlan') },
    { keys: 'Ctrl+M', action: t('dialogs.modeManual') },
    { keys: 'Ctrl+A', action: t('dialogs.modeAuto') },
    { keys: 'Ctrl+H', action: t('dialogs.historyShortcut') },
    { keys: 'Ctrl+S', action: t('dialogs.statsShortcut') },
    { keys: 'Ctrl+,', action: t('dialogs.settingsShortcut') },
    { keys: 'Ctrl+O', action: t('dialogs.openFiles') },
    { keys: '?', action: t('dialogs.shortcutsHelp') },
  ];

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{t('dialogs.shortcutsTitle')}</DialogTitle>
      <DialogContent>
        <Table size="small">
          <TableBody>
            {shortcuts.map((s) => (
              <TableRow key={s.keys}>
                <TableCell sx={{ fontFamily: 'monospace', fontWeight: 600 }}>{s.keys}</TableCell>
                <TableCell>{s.action}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>{t('common.close')}</Button>
      </DialogActions>
    </Dialog>
  );
}
