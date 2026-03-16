import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Divider from '@mui/material/Divider';
import Typography from '@mui/material/Typography';
import FolderOpenIcon from '@mui/icons-material/FolderOpen';
import PlaylistAddCheckIcon from '@mui/icons-material/PlaylistAddCheck';
import DeleteSweepIcon from '@mui/icons-material/DeleteSweep';
import { useNavigate } from 'react-router';
import { useTranslation } from 'react-i18next';
import { useFileStore } from '../../stores/fileStore';
import { useModeStore } from '../../stores/modeStore';
import { useNotification } from '../../utils/notifications';
import { createPlan } from '../../api/planApi';
import DragDropZone from '../files/DragDropZone';
import FileList from '../files/FileList';
import AudioPlayer from '../audio/AudioPlayer';

/**
 * Panneau latéral gauche (sidebar).
 * Permet de :
 * - Sélectionner des fichiers audio via le file picker Electron (Ctrl+O)
 * - Glisser-déposer des fichiers (DragDropZone)
 * - Créer un plan de tagging à partir des fichiers sélectionnés (appel API → navigation vers /plan/:id)
 * - Voir la liste des fichiers sélectionnés (avec suppression individuelle ou globale)
 * - Écouter un fichier sélectionné via le lecteur audio intégré
 */
export default function Sidebar() {
  const navigate = useNavigate();
  const notify = useNotification();
  const { t } = useTranslation();
  const mode = useModeStore((s) => s.mode);
  const files = useFileStore((s) => s.selectedFiles);
  const selectFiles = useFileStore((s) => s.selectFiles);
  const removeFile = useFileStore((s) => s.removeFile);
  const clearFiles = useFileStore((s) => s.clearFiles);
  const selectSingleFile = useFileStore((s) => s.selectSingleFile);

  /** Crée un plan de tagging via l'API puis redirige vers la page de revue du plan. */
  const handleCreatePlan = async () => {
    try {
      const plan = await createPlan(files, mode);
      notify.success(t('sidebar.planCreated', { planId: plan.planId }));
      navigate(`/plan/${plan.planId}`);
    } catch {
      notify.error(t('sidebar.planCreateError'));
    }
  };

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', p: 1, gap: 1 }}>
      {/* En-tête : titre + bouton "Vider" pour supprimer tous les fichiers */}
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <Typography variant="subtitle2">{t('sidebar.files')}</Typography>
        {files.length > 0 && (
          <Button size="small" startIcon={<DeleteSweepIcon />} onClick={clearFiles} color="error">
            {t('sidebar.clear')}
          </Button>
        )}
      </Box>

      {/* Bouton d'ouverture du file picker natif Electron */}
      <Button
        variant="outlined"
        size="small"
        startIcon={<FolderOpenIcon />}
        onClick={selectFiles}
        fullWidth
      >
        {t('sidebar.selectFiles')}
      </Button>

      {/* Zone de drag & drop pour ajouter des fichiers audio */}
      <DragDropZone />

      {/* Bouton "Créer un plan" — visible uniquement quand des fichiers sont sélectionnés */}
      {files.length > 0 && (
        <Button
          variant="contained"
          size="small"
          startIcon={<PlaylistAddCheckIcon />}
          onClick={handleCreatePlan}
          fullWidth
        >
          {t('sidebar.createPlan')}
        </Button>
      )}

      <Divider />

      {/* Liste scrollable des fichiers sélectionnés */}
      <FileList files={files} onRemove={removeFile} onSelect={selectSingleFile} />

      <Divider />

      {/* Lecteur audio HTML5 pour pré-écouter le fichier sélectionné */}
      <AudioPlayer />
    </Box>
  );
}
