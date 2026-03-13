import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import type { ChatMessage } from '../../types/types';
import { formatTimestamp } from '../../utils/helpers';

interface Props {
  message: ChatMessage;
}

/**
 * Bulle de message dans le chat.
 * Affiche un message de l'utilisateur (aligné à droite, fond primaire)
 * ou de l'agent Ayan (aligné à gauche, fond neutre avec bordure).
 * Inclut le nom de l'auteur, le contenu (avec retours à la ligne préservés)
 * et le timestamp formaté.
 */
export default function MessageBubble({ message }: Props) {
  const isUser = message.role === 'user';

  return (
    <Box
      sx={{
        display: 'flex',
        justifyContent: isUser ? 'flex-end' : 'flex-start',
        mb: 1,
      }}
    >
      <Box
        sx={{
          maxWidth: '70%',
          p: 1.5,
          borderRadius: 2,
          bgcolor: isUser ? 'primary.dark' : 'background.paper',
          border: isUser ? 'none' : 1,
          borderColor: 'divider',
        }}
      >
        <Typography variant="caption" color="text.secondary" fontWeight={600}>
          {isUser ? 'Vous' : 'Ayan'}
        </Typography>
        <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', mt: 0.5 }}>
          {message.content}
        </Typography>
        <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5, display: 'block', textAlign: 'right' }}>
          {formatTimestamp(message.timestamp)}
        </Typography>
      </Box>
    </Box>
  );
}
