import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableRow from '@mui/material/TableRow';
import DialogActions from '@mui/material/DialogActions';
import Button from '@mui/material/Button';

/** Liste statique des raccourcis clavier disponibles dans l'application. */
const shortcuts = [
  { keys: 'Ctrl+P', action: 'Mode Plan' },
  { keys: 'Ctrl+M', action: 'Mode Manuel' },
  { keys: 'Ctrl+A', action: 'Mode Auto' },
  { keys: 'Ctrl+H', action: 'Historique' },
  { keys: 'Ctrl+S', action: 'Statistiques' },
  { keys: 'Ctrl+,', action: 'Parametres' },
  { keys: 'Ctrl+O', action: 'Ouvrir fichiers' },
  { keys: '?', action: 'Aide raccourcis' },
];

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
  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Raccourcis clavier</DialogTitle>
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
        <Button onClick={onClose}>Fermer</Button>
      </DialogActions>
    </Dialog>
  );
}
