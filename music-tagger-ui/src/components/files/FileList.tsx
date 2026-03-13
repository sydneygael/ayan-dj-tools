import List from '@mui/material/List';
import Typography from '@mui/material/Typography';
import FileItem from './FileItem';

interface Props {
  files: string[];
  onRemove: (filepath: string) => void;
  onSelect: (filepath: string) => void;
}

/**
 * Liste des fichiers audio sélectionnés dans la sidebar.
 * Affiche un compteur du nombre de fichiers et une liste scrollable de FileItem.
 * Si aucun fichier n'est sélectionné, affiche un message d'indication.
 */
export default function FileList({ files, onRemove, onSelect }: Props) {
  if (files.length === 0) {
    return (
      <Typography variant="body2" color="text.secondary" sx={{ p: 1, textAlign: 'center' }}>
        Aucun fichier selectionne
      </Typography>
    );
  }

  return (
    <>
      <Typography variant="caption" color="text.secondary" sx={{ px: 1 }}>
        {files.length} fichier{files.length > 1 ? 's' : ''}
      </Typography>
      <List dense sx={{ overflow: 'auto', flex: 1 }}>
        {files.map((f) => (
          <FileItem key={f} filepath={f} onRemove={onRemove} onSelect={onSelect} />
        ))}
      </List>
    </>
  );
}
