import { create } from 'zustand';
import type { ChatMessage } from '../types/types';

/** État de la conversation de chat avec l'agent Ayan. */
interface ChatState {
  /** Historique des messages de la conversation courante (user + agent). */
  messages: ChatMessage[];
  /** ID de la conversation côté backend (Redis), null si pas encore initialisée. */
  conversationId: string | null;
  /** Indicateur de chargement : true quand on attend la réponse de l'agent. */
  loading: boolean;
  /** Ajoute un message (user ou agent) à l'historique local. */
  addMessage: (msg: ChatMessage) => void;
  /** Met à jour l'ID de conversation reçu du backend. */
  setConversationId: (id: string) => void;
  /** Active/désactive le spinner de chargement. */
  setLoading: (loading: boolean) => void;
  /** Réinitialise la conversation (messages, ID, loading). */
  clear: () => void;
}

/**
 * Store Zustand de la conversation de chat.
 * Non persisté : la conversation repart à zéro à chaque session.
 * L'historique côté serveur est conservé dans Redis (TTL 24h).
 */
export const useChatStore = create<ChatState>()((set) => ({
  messages: [],
  conversationId: null,
  loading: false,

  addMessage: (msg) => set((s) => ({ messages: [...s.messages, msg] })),
  setConversationId: (id) => set({ conversationId: id }),
  setLoading: (loading) => set({ loading }),
  clear: () => set({ messages: [], conversationId: null, loading: false }),
}));
