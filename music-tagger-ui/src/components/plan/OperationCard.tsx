import { useMemo } from 'react';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import CardActions from '@mui/material/CardActions';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import type { TagOperation } from '../../types/types';
import { OperationStatus } from '../../types/types';
import { extractFilename } from '../../utils/helpers';

interface Props {
  operation: TagOperation;
  onApprove: (filepath: string) => void;
  onReject: (filepath: string) => void;
}

/**
 * Carte d'une opération de tagging dans la page de revue d'un plan.
 * Affiche un tableau comparatif (tags actuels vs suggérés) avec mise en évidence
 * des différences (barré rouge pour l'ancien, gras vert pour le nouveau).
 * Le chip de statut (PENDING/APPROVED/REJECTED/APPLIED/FAILED) est coloré.
 * Les boutons Approuver/Rejeter ne sont visibles que pour les opérations en attente (PENDING).
 */
export default function OperationCard({ operation, onApprove, onReject }: Props) {
  // Fusionne les clés de tags actuels et suggérés pour afficher toutes les lignes du diff
  const allTags = useMemo(() => {
    const keys = new Set([
      ...Object.keys(operation.currentTags),
      ...Object.keys(operation.suggestedTags),
    ]);
    return Array.from(keys).sort();
  }, [operation]);

  const isPending = operation.status === OperationStatus.PENDING;

  /** Mapping statut → couleur MUI pour le Chip. */
  const statusColor: Record<string, 'default' | 'primary' | 'error' | 'success' | 'warning'> = {
    PENDING: 'default',
    APPROVED: 'primary',
    REJECTED: 'error',
    APPLIED: 'success',
    FAILED: 'warning',
  };

  return (
    <Card variant="outlined" sx={{ mb: 1 }}>
      <CardContent sx={{ pb: 1 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
          <Typography variant="subtitle2">{extractFilename(operation.filepath)}</Typography>
          <Chip label={operation.status} color={statusColor[operation.status] ?? 'default'} size="small" />
        </Box>

        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell sx={{ fontWeight: 600 }}>Tag</TableCell>
              <TableCell sx={{ fontWeight: 600 }}>Actuel</TableCell>
              <TableCell sx={{ fontWeight: 600 }}>Suggere</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {allTags.map((tag) => {
              const current = operation.currentTags[tag] ?? '';
              const suggested = operation.suggestedTags[tag] ?? '';
              const changed = current !== suggested;
              return (
                <TableRow key={tag}>
                  <TableCell>{tag}</TableCell>
                  <TableCell
                    sx={changed ? { textDecoration: 'line-through', color: 'error.main' } : {}}
                  >
                    {current || '-'}
                  </TableCell>
                  <TableCell sx={changed ? { fontWeight: 600, color: 'success.main' } : {}}>
                    {suggested || '-'}
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>

        {operation.message && (
          <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
            {operation.message}
          </Typography>
        )}
      </CardContent>

      {isPending && (
        <CardActions sx={{ justifyContent: 'flex-end' }}>
          <Button size="small" color="error" onClick={() => onReject(operation.filepath)}>
            Rejeter
          </Button>
          <Button size="small" variant="contained" onClick={() => onApprove(operation.filepath)}>
            Approuver
          </Button>
        </CardActions>
      )}
    </Card>
  );
}
