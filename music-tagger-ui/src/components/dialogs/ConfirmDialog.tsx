import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogActions from '@mui/material/DialogActions';
import Button from '@mui/material/Button';
import type { ConfirmDialogData } from '../../types/types';

interface Props {
  open: boolean;
  onClose: () => void;
  data: ConfirmDialogData;
  onConfirm: () => void;
}

/**
 * Dialog de confirmation générique réutilisable.
 * Affiche un titre, un message, et deux boutons (annuler/confirmer).
 * Le bouton de confirmation est rouge (color="error") si `data.warn` est true,
 * pour les actions destructives (suppression, exécution irréversible).
 * Les labels des boutons sont personnalisables via data.cancelLabel / data.confirmLabel.
 */
export default function ConfirmDialog({ open, onClose, data, onConfirm }: Props) {
  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{data.title}</DialogTitle>
      <DialogContent>
        <DialogContentText>{data.message}</DialogContentText>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>{data.cancelLabel ?? 'Annuler'}</Button>
        <Button
          variant="contained"
          color={data.warn ? 'error' : 'primary'}
          onClick={onConfirm}
        >
          {data.confirmLabel ?? 'Confirmer'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
