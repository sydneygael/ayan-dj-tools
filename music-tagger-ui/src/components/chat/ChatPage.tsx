import { useCallback, useEffect, useRef, useState } from 'react';
import Box from '@mui/material/Box';
import TextField from '@mui/material/TextField';
import IconButton from '@mui/material/IconButton';
import Typography from '@mui/material/Typography';
import CircularProgress from '@mui/material/CircularProgress';
import SendIcon from '@mui/icons-material/Send';
import SmartToyIcon from '@mui/icons-material/SmartToy';
import { useTranslation } from 'react-i18next';
import { useChatStore } from '../../stores/chatStore';
import { useWebSocket } from '../../hooks/useWebSocket';
import { chat as chatRest } from '../../api/agentApi';
import MessageBubble from './MessageBubble';
import SuggestedQuestions from './SuggestedQuestions';
import WsStatusChip from '../common/WsStatusChip';

/**
 * Page de chat principal avec l'agent Ayan.
 * Communique via WebSocket STOMP (prioritaire) avec fallback REST.
 * Gère l'historique des messages (store Zustand), l'envoi (Enter ou bouton),
 * le défilement automatique et l'affichage du spinner pendant le chargement.
 */
export default function ChatPage() {
  const [input, setInput] = useState('');
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const { messages, conversationId, loading, addMessage, setConversationId, setLoading } =
    useChatStore();
  const ws = useWebSocket();
  const { t } = useTranslation();

  // Connexion WebSocket au montage du composant.
  useEffect(() => {
    ws.connect();
    return () => ws.disconnect();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Réception des messages WebSocket
  useEffect(() => {
    if (!ws.lastMessage) return;
    const msg = ws.lastMessage;
    addMessage({ role: 'agent', content: msg.reply, timestamp: msg.timestamp });
    setConversationId(msg.conversationId);
    setLoading(false);
  }, [ws.lastMessage, addMessage, setConversationId, setLoading]);

  // Défilement automatique vers le bas à chaque nouveau message
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
          content: t('chat.connectionError'),
          timestamp: new Date().toISOString(),
        });
      } finally {
        setLoading(false);
      }
    }
  }, [input, loading, ws, conversationId, addMessage, setConversationId, setLoading, t]);

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
          <Box sx={{ textAlign: 'center', mt: 8 }}>
            <Box sx={{ opacity: 0.6 }}>
              <SmartToyIcon sx={{ fontSize: 64, color: 'primary.main' }} />
              <Typography variant="h6" sx={{ mt: 2 }}>
                {t('chat.greeting')}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {t('chat.subtitle')}
              </Typography>
            </Box>
            <SuggestedQuestions
              onSelect={(q) => {
                setInput(q);
                inputRef.current?.focus();
              }}
            />
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

      <Box sx={{ display: 'flex', gap: 1, p: 1, borderTop: 1, borderColor: 'divider', alignItems: 'center' }}>
        <WsStatusChip connected={ws.connected} />
        <TextField
          fullWidth
          size="small"
          placeholder={t('chat.placeholder')}
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          multiline
          maxRows={3}
          inputRef={inputRef}
        />
        <IconButton color="primary" onClick={send} disabled={!input.trim() || loading} aria-label={t('chat.sendLabel')}>
          <SendIcon />
        </IconButton>
      </Box>
    </Box>
  );
}
