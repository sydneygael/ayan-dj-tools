import { useState } from 'react';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import Button from '@mui/material/Button';
import FormControlLabel from '@mui/material/FormControlLabel';
import Checkbox from '@mui/material/Checkbox';
import RadioGroup from '@mui/material/RadioGroup';
import Radio from '@mui/material/Radio';
import Typography from '@mui/material/Typography';
import type { AgentQuestion, AgentQuestionResponse } from '../../types/types';

interface Props {
  open: boolean;
  onClose: () => void;
  question: AgentQuestion;
  onSubmit: (response: AgentQuestionResponse) => void;
}

/**
 * Dialog de question posée par l'agent Ayan pendant le traitement.
 * L'agent peut demander à l'utilisateur de choisir parmi des options (radio buttons)
 * pour résoudre une ambiguïté (ex: plusieurs artistes possibles).
 * Inclut une checkbox "Appliquer aux fichiers similaires" pour éviter de reposer
 * la même question pour des cas identiques.
 * Retourne un AgentQuestionResponse avec l'option choisie et le flag applyToSimilar.
 */
export default function AgentQuestionDialog({ open, onClose, question, onSubmit }: Props) {
  const [selected, setSelected] = useState('');
  const [applyToSimilar, setApplyToSimilar] = useState(false);

  /** Soumet la réponse et ferme la dialog. Ne fait rien si aucune option n'est sélectionnée. */
  const handleSubmit = () => {
    if (!selected) return;
    onSubmit({
      questionId: question.questionId,
      selectedOption: selected,
      applyToSimilar,
    });
    onClose();
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{question.question}</DialogTitle>
      <DialogContent>
        {question.context && (
          <Typography variant="body2" color="text.secondary" gutterBottom>
            {question.context}
          </Typography>
        )}
        <RadioGroup value={selected} onChange={(e) => setSelected(e.target.value)}>
          {question.options.map((opt) => (
            <FormControlLabel key={opt} value={opt} control={<Radio />} label={opt} />
          ))}
        </RadioGroup>
        <FormControlLabel
          control={
            <Checkbox checked={applyToSimilar} onChange={(e) => setApplyToSimilar(e.target.checked)} />
          }
          label="Appliquer aux fichiers similaires"
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Annuler</Button>
        <Button variant="contained" onClick={handleSubmit} disabled={!selected}>
          Repondre
        </Button>
      </DialogActions>
    </Dialog>
  );
}
