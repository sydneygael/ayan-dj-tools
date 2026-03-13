import { useCallback, useState } from 'react';
import { useSearchParams } from 'react-router';
import Box from '@mui/material/Box';
import TextField from '@mui/material/TextField';
import Button from '@mui/material/Button';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import CircularProgress from '@mui/material/CircularProgress';
import Typography from '@mui/material/Typography';
import Collapse from '@mui/material/Collapse';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ErrorIcon from '@mui/icons-material/Error';
import SearchIcon from '@mui/icons-material/Search';
import { getPlanHistory } from '../../api/planApi';
import type { TaggingHistoryEntry } from '../../types/types';
import { extractFilename } from '../../utils/helpers';

/**
 * Page d'historique des modifications de tags (route /history).
 * Recherche par Plan ID : appelle l'API pour récupérer les entrées d'historique.
 * Affiche un tableau avec statut (succès/échec), nom de fichier, nombre de modifications et date.
 * Chaque ligne est extensible (Collapse) pour voir le détail des changements (tag : ancien → nouveau).
 * Le champ Plan ID est pré-rempli si le query param ?planId= est présent dans l'URL.
 */
export default function HistoryPage() {
  const [searchParams] = useSearchParams();
  // Pré-remplissage du champ de recherche depuis le query param (ex: navigation depuis la page plan)
  const [planId, setPlanId] = useState(searchParams.get('planId') ?? '');
  const [entries, setEntries] = useState<TaggingHistoryEntry[]>([]);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);
  const [expandedIdx, setExpandedIdx] = useState<number | null>(null);

  /** Recherche les entrées d'historique pour le plan ID saisi. */
  const search = useCallback(async () => {
    if (!planId.trim()) return;
    setLoading(true);
    setSearched(true);
    try {
      setEntries(await getPlanHistory(planId.trim()));
    } catch {
      setEntries([]);
    } finally {
      setLoading(false);
    }
  }, [planId]);

  return (
    <Box sx={{ maxWidth: 900, mx: 'auto' }}>
      <Typography variant="h6" gutterBottom>
        Historique des modifications
      </Typography>

      <Box sx={{ display: 'flex', gap: 1, mb: 2 }}>
        <TextField
          size="small"
          label="Plan ID"
          value={planId}
          onChange={(e) => setPlanId(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && search()}
          sx={{ flex: 1 }}
        />
        <Button variant="contained" startIcon={<SearchIcon />} onClick={search} disabled={loading}>
          Rechercher
        </Button>
      </Box>

      {loading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
          <CircularProgress />
        </Box>
      )}

      {searched && !loading && entries.length === 0 && (
        <Typography color="text.secondary" sx={{ textAlign: 'center', py: 4 }}>
          Aucun resultat
        </Typography>
      )}

      {entries.length > 0 && (
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Statut</TableCell>
              <TableCell>Fichier</TableCell>
              <TableCell>Modifications</TableCell>
              <TableCell>Date</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {entries.map((entry, idx) => (
              <>
                <TableRow
                  key={idx}
                  hover
                  sx={{ cursor: 'pointer' }}
                  onClick={() => setExpandedIdx(expandedIdx === idx ? null : idx)}
                >
                  <TableCell>
                    {entry.success ? (
                      <CheckCircleIcon fontSize="small" color="success" />
                    ) : (
                      <ErrorIcon fontSize="small" color="error" />
                    )}
                  </TableCell>
                  <TableCell>{extractFilename(entry.filepath)}</TableCell>
                  <TableCell>{entry.changes.length}</TableCell>
                  <TableCell>{new Date(entry.appliedAt).toLocaleString('fr-FR')}</TableCell>
                </TableRow>
                <TableRow key={`${idx}-detail`}>
                  <TableCell colSpan={4} sx={{ p: 0 }}>
                    <Collapse in={expandedIdx === idx}>
                      <Box sx={{ p: 2 }}>
                        {entry.changes.map((c, ci) => (
                          <Typography key={ci} variant="body2">
                            <strong>{c.tagName}</strong>: {c.oldValue ?? '(vide)'} → {c.newValue}
                          </Typography>
                        ))}
                      </Box>
                    </Collapse>
                  </TableCell>
                </TableRow>
              </>
            ))}
          </TableBody>
        </Table>
      )}
    </Box>
  );
}
