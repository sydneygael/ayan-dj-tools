import { useCallback, useEffect, useRef, useState } from 'react';
import Box from '@mui/material/Box';
import IconButton from '@mui/material/IconButton';
import Slider from '@mui/material/Slider';
import Typography from '@mui/material/Typography';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import PauseIcon from '@mui/icons-material/Pause';
import VolumeUpIcon from '@mui/icons-material/VolumeUp';
import { useTranslation } from 'react-i18next';
import { useFileStore } from '../../stores/fileStore';
import { extractFilename, formatTime } from '../../utils/helpers';

/**
 * Lecteur audio intégré dans la sidebar.
 * Lit le fichier sélectionné via le protocole file:// (Electron).
 * Contrôles : play/pause, barre de progression (seek), volume.
 * Se réinitialise automatiquement quand le fichier sélectionné change.
 * Masqué (return null) quand aucun fichier n'est sélectionné.
 */
export default function AudioPlayer() {
  const selectedFile = useFileStore((s) => s.selectedSingleFile);
  const audioRef = useRef<HTMLAudioElement>(null);
  const [playing, setPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [volume, setVolume] = useState(80);
  const { t } = useTranslation();

  // Réinitialisation du lecteur quand le fichier sélectionné change.
  useEffect(() => {
    setPlaying(false);
    setCurrentTime(0);
    setDuration(0);
  }, [selectedFile]);

  const togglePlay = useCallback(() => {
    const audio = audioRef.current;
    if (!audio) return;
    if (playing) {
      audio.pause();
    } else {
      audio.play();
    }
    setPlaying(!playing);
  }, [playing]);

  const handleSeek = useCallback((_: unknown, v: number | number[]) => {
    const audio = audioRef.current;
    if (!audio) return;
    const val = v as number;
    audio.currentTime = val;
    setCurrentTime(val);
  }, []);

  const handleVolume = useCallback((_: unknown, v: number | number[]) => {
    const audio = audioRef.current;
    if (!audio) return;
    const val = v as number;
    audio.volume = val / 100;
    setVolume(val);
  }, []);

  if (!selectedFile) return null;

  return (
    <Box sx={{ px: 1, py: 0.5 }}>
      <audio
        ref={audioRef}
        src={`file:///${selectedFile.replace(/\\/g, '/')}`}
        onTimeUpdate={() => setCurrentTime(audioRef.current?.currentTime ?? 0)}
        onLoadedMetadata={() => setDuration(audioRef.current?.duration ?? 0)}
        onEnded={() => setPlaying(false)}
      />
      <Typography variant="caption" noWrap display="block" color="text.secondary">
        {extractFilename(selectedFile)}
      </Typography>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
        <IconButton size="small" onClick={togglePlay} aria-label={playing ? t('audio.pause') : t('audio.play')}>
          {playing ? <PauseIcon fontSize="small" /> : <PlayArrowIcon fontSize="small" />}
        </IconButton>
        <Typography variant="caption" sx={{ minWidth: 36 }}>
          {formatTime(currentTime)}
        </Typography>
        <Slider
          size="small"
          value={currentTime}
          max={duration || 1}
          onChange={handleSeek}
          aria-label={t('audio.seekLabel')}
          sx={{ mx: 0.5 }}
        />
        <Typography variant="caption" sx={{ minWidth: 36 }}>
          {formatTime(duration)}
        </Typography>
      </Box>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, mt: 0.5 }}>
        <VolumeUpIcon fontSize="small" color="action" />
        <Slider size="small" value={volume} onChange={handleVolume} aria-label={t('audio.volumeLabel')} sx={{ width: 80 }} />
      </Box>
    </Box>
  );
}
