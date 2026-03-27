import { create } from 'zustand';
import type { ChatMessage } from '../types/types';

/** État de la conversation de chat avec l'agent Ayan. */
interface ChatState {
  messages: ChatMessage[];
  /** ID de conversation, généré côté frontend (UUID) et stable pour toute la session. */
  conversationId: string;
  loading: boolean;
  /** Contenu en cours de streaming (null si aucune génération en cours). */
  streamingContent: string | null;
  addMessage: (msg: ChatMessage) => void;
  appendStreamChunk: (chunk: string) => void;
  finalizeStream: (reply: string, timestamp: string) => void;
  setConversationId: (id: string) => void;
  setLoading: (loading: boolean) => void;
  clear: () => void;
}

export const useChatStore = create<ChatState>()((set) => ({
  messages: [],
  conversationId: crypto.randomUUID(),
  loading: false,
  streamingContent: null,

  addMessage: (msg) => set((s) => ({ messages: [...s.messages, msg] })),

  /** Appelé à chaque token reçu — arrête le spinner et alimente la bulle de streaming. */
  appendStreamChunk: (chunk) =>
    set((s) => ({ streamingContent: (s.streamingContent ?? '') + chunk, loading: false })),

  /** Appelé quand le stream est terminé — transforme le contenu streamé en message définitif. */
  finalizeStream: (reply, timestamp) =>
    set((s) => ({
      messages: [...s.messages, { role: 'agent', content: reply, timestamp }],
      streamingContent: null,
      loading: false,
    })),

  setConversationId: (id) => set({ conversationId: id }),
  setLoading: (loading) => set({ loading }),
  clear: () => set({ messages: [], conversationId: crypto.randomUUID(), streamingContent: null, loading: false }),
}));
