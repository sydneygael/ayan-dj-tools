/**
 * Client API REST pour l'agent Ayan.
 * Communique avec AgentController (POST /api/agent/chat, GET/DELETE /conversations).
 * Utilisé en fallback quand la connexion WebSocket n'est pas disponible.
 */
import { environment } from '../config/environment';
import type { ChatMessage, ChatResponse } from '../types/types';

/** URL de base de l'API agent. */
const BASE = `${environment.apiUrl}/api/agent`;

/** Envoie un message à l'agent et retourne sa réponse. Crée ou reprend une conversation. */
export async function chat(message: string, conversationId?: string): Promise<ChatResponse> {
  const res = await fetch(`${BASE}/chat`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message, conversationId }),
  });
  if (!res.ok) throw new Error(`Chat failed: ${res.status}`);
  return res.json();
}

/** Récupère l'historique complet des messages d'une conversation. */
export async function getHistory(conversationId: string): Promise<ChatMessage[]> {
  const res = await fetch(`${BASE}/conversations/${conversationId}/history`);
  if (!res.ok) throw new Error(`Get history failed: ${res.status}`);
  return res.json();
}

/** Supprime une conversation et son historique côté serveur (Redis). */
export async function deleteConversation(conversationId: string): Promise<void> {
  const res = await fetch(`${BASE}/conversations/${conversationId}`, { method: 'DELETE' });
  if (!res.ok) throw new Error(`Delete conversation failed: ${res.status}`);
}
