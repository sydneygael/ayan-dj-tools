import IconButton from '@mui/material/IconButton';
import ListItem from '@mui/material/ListItem';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import Tooltip from '@mui/material/Tooltip';
import AudioFileIcon from '@mui/icons-material/AudioFile';
import CloseIcon from '@mui/icons-material/Close';
import { useTranslation } from 'react-i18next';
import { extractFilename } from '../../utils/helpers';

interface Props {
  filepath: string;
  onRemove: (filepath: string) => void;
  onSelect: (filepath: string) => void;
}

/**
 * Élément individuel dans la liste des fichiers sélectionnés.
 * Affiche le nom du fichier (extrait du chemin complet), une icône audio,
 * un bouton de suppression (croix) et gère le clic pour sélectionner le fichier
 * dans le lecteur audio. Le chemin complet est visible en tooltip (attribut title).
 */
export default function FileItem({ filepath, onRemove, onSelect }: Props) {
  const { t } = useTranslation();

  return (
    <ListItem
      disablePadding
      secondaryAction={
        <IconButton edge="end" size="small" onClick={() => onRemove(filepath)} aria-label={t('files.removeFile')}>
          <CloseIcon fontSize="small" />
        </IconButton>
      }
    >
      <Tooltip title={filepath} placement="right" arrow>
        <ListItemButton dense onClick={() => onSelect(filepath)}>
          <ListItemIcon sx={{ minWidth: 32 }}>
            <AudioFileIcon fontSize="small" color="primary" />
          </ListItemIcon>
          <ListItemText
            primary={extractFilename(filepath)}
            primaryTypographyProps={{ noWrap: true, fontSize: '0.85rem' }}
          />
        </ListItemButton>
      </Tooltip>
    </ListItem>
  );
}
