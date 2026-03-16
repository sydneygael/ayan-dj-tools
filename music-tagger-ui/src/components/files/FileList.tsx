import List from '@mui/material/List';
import Typography from '@mui/material/Typography';
import { useTranslation } from 'react-i18next';
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
  const { t } = useTranslation();

  if (files.length === 0) {
    return (
      <Typography variant="body2" color="text.secondary" sx={{ p: 1, textAlign: 'center' }}>
        {t('files.noFilesSelected')}
      </Typography>
    );
  }

  return (
    <>
      <Typography variant="caption" color="text.secondary" sx={{ px: 1 }}>
        {t('files.count', { count: files.length })}
      </Typography>
      <List dense sx={{ overflow: 'auto', flex: 1 }}>
        {files.map((f) => (
          <FileItem key={f} filepath={f} onRemove={onRemove} onSelect={onSelect} />
        ))}
      </List>
    </>
  );
}
