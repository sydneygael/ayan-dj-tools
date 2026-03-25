import { useState } from 'react';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Button from '@mui/material/Button';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemText from '@mui/material/ListItemText';
import Chip from '@mui/material/Chip';
import CircularProgress from '@mui/material/CircularProgress';
import Alert from '@mui/material/Alert';
import Divider from '@mui/material/Divider';
import QueueMusicIcon from '@mui/icons-material/QueueMusic';
import { generatePlaylist } from '../../api/playlistApi';
import type { Playlist, EnrichedTrackMetadata } from '../../types/types';

function TrackChips({ track }: { track: EnrichedTrackMetadata }) {
  const af = track.audioFeatures;
  return (
    <Stack direction="row" spacing={0.5} flexWrap="wrap" sx={{ mt: 0.5 }}>
      {af?.bpm != null && (
        <Chip label={`${Math.round(af.bpm)} BPM`} size="small" color="primary" variant="outlined" />
      )}
      {af?.musicalKey && (
        <Chip label={`${af.musicalKey}${af.mode ? ' ' + af.mode : ''}`} size="small" variant="outlined" />
      )}
      {track.genres[0] && (
        <Chip label={track.genres[0]} size="small" variant="outlined" />
      )}
      {af?.danceability != null && (
        <Chip label={`Dance ${Math.round(af.danceability * 100)}%`} size="small" variant="outlined" />
      )}
    </Stack>
  );
}

export default function PlaylistPage() {
  const [bpmMin, setBpmMin] = useState(120);
  const [bpmMax, setBpmMax] = useState(145);
  const [genre, setGenre] = useState('');
  const [playlist, setPlaylist] = useState<Playlist | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleGenerate = async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await generatePlaylist({ bpmMin, bpmMax, genre });
      setPlaylist(result);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erreur lors de la generation');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box sx={{ p: 3, maxWidth: 800, mx: 'auto' }}>
      <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 3 }}>
        <QueueMusicIcon color="primary" />
        <Typography variant="h5" fontWeight={600}>
          Playlist 3/4 Loop Mixing
        </Typography>
      </Stack>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Stack direction="row" spacing={2} alignItems="flex-end" flexWrap="wrap">
            <TextField
              label="BPM min"
              type="number"
              value={bpmMin}
              onChange={(e) => setBpmMin(Number(e.target.value))}
              size="small"
              sx={{ width: 100 }}
              inputProps={{ min: 60, max: 200 }}
            />
            <TextField
              label="BPM max"
              type="number"
              value={bpmMax}
              onChange={(e) => setBpmMax(Number(e.target.value))}
              size="small"
              sx={{ width: 100 }}
              inputProps={{ min: 60, max: 200 }}
            />
            <TextField
              label="Genre (optionnel)"
              value={genre}
              onChange={(e) => setGenre(e.target.value)}
              size="small"
              placeholder="house, techno..."
              sx={{ width: 180 }}
            />
            <Button
              variant="contained"
              onClick={handleGenerate}
              disabled={loading || bpmMin >= bpmMax}
              startIcon={loading ? <CircularProgress size={16} color="inherit" /> : <QueueMusicIcon />}
            >
              Generer 10 tracks
            </Button>
          </Stack>
        </CardContent>
      </Card>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {playlist && playlist.tracks.length === 0 && (
        <Alert severity="info">
          Aucun track trouve. Enrichis d'abord des fichiers audio via Ayan pour alimenter le vector store.
        </Alert>
      )}

      {playlist && playlist.tracks.length > 0 && (
        <Card>
          <CardContent sx={{ pb: 0 }}>
            <Typography variant="subtitle2" color="text.secondary" gutterBottom>
              {playlist.tracks.length} track{playlist.tracks.length > 1 ? 's' : ''} — {playlist.name}
            </Typography>
          </CardContent>
          <List disablePadding>
            {playlist.tracks.map((track, idx) => (
              <Box key={track.sourceId}>
                {idx > 0 && <Divider />}
                <ListItem alignItems="flex-start">
                  <Typography
                    variant="body2"
                    color="text.secondary"
                    sx={{ minWidth: 28, pt: 0.5 }}
                  >
                    {idx + 1}.
                  </Typography>
                  <ListItemText
                    primary={
                      <Typography variant="body1" fontWeight={500}>
                        {track.artist} — {track.title}
                      </Typography>
                    }
                    secondary={
                      <>
                        <Typography variant="body2" color="text.secondary">
                          {track.album}{track.releaseYear > 0 ? ` · ${track.releaseYear}` : ''}
                        </Typography>
                        <TrackChips track={track} />
                      </>
                    }
                    disableTypography
                  />
                </ListItem>
              </Box>
            ))}
          </List>
        </Card>
      )}
    </Box>
  );
}
