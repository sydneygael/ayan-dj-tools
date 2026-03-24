import Box from '@mui/material/Box';
import Chip from '@mui/material/Chip';
import { useTranslation } from 'react-i18next';

interface Props {
  onSelect: (question: string) => void;
}

export default function SuggestedQuestions({ onSelect }: Props) {
  const { t } = useTranslation();
  const suggestions = t('chat.suggestions', { returnObjects: true }) as string[];

  return (
    <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, justifyContent: 'center', mt: 3 }}>
      {suggestions.map((q, i) => (
        <Chip
          key={i}
          label={q}
          variant="outlined"
          clickable
          onClick={() => onSelect(q)}
          sx={{ fontSize: '0.8rem' }}
        />
      ))}
    </Box>
  );
}
