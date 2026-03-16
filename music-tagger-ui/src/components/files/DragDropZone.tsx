import { useCallback, useState } from 'react';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import { useTranslation } from 'react-i18next';
import { isAudioFile } from '../../utils/helpers';
import { useFileStore } from '../../stores/fileStore';

/**
 * Zone de glisser-déposer (drag & drop) pour ajouter des fichiers audio.
 * Filtre les fichiers par extension audio (via isAudioFile) et récupère
 * le chemin absolu via l'API Electron (File.path).
 * L'état `dragging` gère le feedback visuel (bordure + fond surligné).
 */
export default function DragDropZone() {
  const [dragging, setDragging] = useState(false);
  const addFiles = useFileStore((s) => s.addFiles);
  const { t } = useTranslation();

  /** Traite le drop : filtre les fichiers audio, extrait les chemins, les ajoute au store. */
  const handleDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault();
      setDragging(false);
      const files = Array.from(e.dataTransfer.files)
        .filter((f) => isAudioFile(f.name))
        .map((f) => (f as File & { path: string }).path);
      if (files.length > 0) addFiles(files);
    },
    [addFiles],
  );

  return (
    <Box
      onDragOver={(e) => {
        e.preventDefault();
        setDragging(true);
      }}
      onDragLeave={() => setDragging(false)}
      onDrop={handleDrop}
      sx={{
        border: '2px dashed',
        borderColor: dragging ? 'primary.main' : 'divider',
        borderRadius: 1,
        p: 2,
        textAlign: 'center',
        cursor: 'pointer',
        transition: 'border-color 0.2s',
        bgcolor: dragging ? 'action.hover' : 'transparent',
      }}
    >
      <CloudUploadIcon color={dragging ? 'primary' : 'disabled'} />
      <Typography variant="caption" display="block" color="text.secondary">
        {t('files.dragDrop')}
      </Typography>
    </Box>
  );
}
