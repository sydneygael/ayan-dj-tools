import { useCallback, useEffect, useRef, useState } from 'react';
import Box from '@mui/material/Box';
import TextField from '@mui/material/TextField';
import IconButton from '@mui/material/IconButton';
import Typography from '@mui/material/Typography';
import CircularProgress from '@mui/material/CircularProgress';
import SendIcon from '@mui/icons-material/Send';
import SmartToyIcon from '@mui/icons-material/SmartToy';
import { useChatStore } from '../../stores/chatStore';
import { useWebSocket } from '../../hooks/useWebSocket';
import { chat as chatRest } from '../../api/agentApi';
import MessageBubble from './MessageBubble';

/**
 * Page de chat principal avec l'agent Ayan.
 * Communique via WebSocket STOMP (prioritaire) avec fallback REST.
 * Gère l'historique des messages (store Zustand), l'envoi (Enter ou bouton),
 * le défilement automatique et l'affichage du spinner pendant le chargement.
 */
export default function ChatPage() {
  const [input, setInput] = useState('');
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const { messages, conversationId, loading, addMessage, setConversationId, setLoading } =
    useChatStore();
  const ws = useWebSocket();

  // Connexion WebSocket au montage du composant.
  // Le cleanup déconnecte proprement lors du démontage pour éviter les fuites de connexion.
  useEffect(() => {
    ws.connect();
    return () => ws.disconnect();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Réception des messages WebSocket : chaque fois que lastMessage change,
  // on ajoute la réponse de l'agent au store et on met à jour l'ID de conversation.
  useEffect(() => {
    if (!ws.lastMessage) return;
    const msg = ws.lastMessage;
    addMessage({ role: 'agent', content: msg.reply, timestamp: msg.timestamp });
    setConversationId(msg.conversationId);
    setLoading(false);
  }, [ws.lastMessage, addMessage, setConversationId, setLoading]);

  // Défilement automatique vers le bas à chaque nouveau message,
  // pour que l'utilisateur voie toujours le dernier message affiché.
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const send = useCallback(async () => {
    const text = input.trim();
    if (!text || loading) return;

    setInput('');
    addMessage({ role: 'user', content: text, timestamp: new Date().toISOString() });
    setLoading(true);

    if (ws.connected) {
      ws.sendMessage(text, conversationId ?? undefined);
    } else {
      try {
        const res = await chatRest(text, conversationId ?? undefined);
        addMessage({ role: 'agent', content: res.reply, timestamp: res.timestamp });
        setConversationId(res.conversationId);
      } catch {
        addMessage({
          role: 'agent',
          content: 'Erreur de connexion au serveur.',
          timestamp: new Date().toISOString(),
        });
      } finally {
        setLoading(false);
      }
    }
  }, [input, loading, ws, conversationId, addMessage, setConversationId, setLoading]);

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      send();
    }
  };

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <Box sx={{ flex: 1, overflow: 'auto', p: 2 }}>
        {messages.length === 0 && (
          <Box sx={{ textAlign: 'center', mt: 8, opacity: 0.6 }}>
            <SmartToyIcon sx={{ fontSize: 64, color: 'primary.main' }} />
            <Typography variant="h6" sx={{ mt: 2 }}>
              Salut ! Je suis Ayan
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Ton assistant DJ. Envoie-moi un message pour commencer.
            </Typography>
          </Box>
        )}
        {messages.map((msg, i) => (
          <MessageBubble key={i} message={msg} />
        ))}
        {loading && (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 2 }}>
            <CircularProgress size={24} />
          </Box>
        )}
        <div ref={messagesEndRef} />
      </Box>

      <Box sx={{ display: 'flex', gap: 1, p: 1, borderTop: 1, borderColor: 'divider' }}>
        <TextField
          fullWidth
          size="small"
          placeholder="Ecrire un message..."
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          multiline
          maxRows={3}
        />
        <IconButton color="primary" onClick={send} disabled={!input.trim() || loading}>
          <SendIcon />
        </IconButton>
      </Box>
    </Box>
  );
}
