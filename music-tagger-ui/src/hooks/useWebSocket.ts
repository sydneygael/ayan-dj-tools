import { useCallback, useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { environment } from '../config/environment';
import type { ChatStreamEvent } from '../types/types';

/**
 * Hook de connexion WebSocket STOMP vers le backend.
 * S'abonne au topic /topic/responses/{conversationId} pour recevoir
 * les événements de streaming (chunk, done, error) propres à la conversation.
 */
export function useWebSocket(conversationId: string) {
  const clientRef = useRef<Client | null>(null);
  const subRef = useRef<{ unsubscribe: () => void } | null>(null);
  const convIdRef = useRef(conversationId);
  const [connected, setConnected] = useState(false);
  const [lastEvent, setLastEvent] = useState<ChatStreamEvent | null>(null);

  // Maintient la ref à jour pour les callbacks STOMP (évite les closures périmées)
  useEffect(() => {
    convIdRef.current = conversationId;
  }, [conversationId]);

  const subscribeToConversation = useCallback((client: Client, convId: string) => {
    subRef.current?.unsubscribe();
    subRef.current = client.subscribe(`/topic/responses/${convId}`, (frame) => {
      setLastEvent(JSON.parse(frame.body) as ChatStreamEvent);
    });
  }, []);

  const connect = useCallback(() => {
    if (clientRef.current?.active) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(environment.wsUrl),
      reconnectDelay: 5000,
      onConnect: () => {
        setConnected(true);
        subscribeToConversation(client, convIdRef.current);
      },
      onDisconnect: () => setConnected(false),
      onStompError: () => setConnected(false),
    });

    client.activate();
    clientRef.current = client;
  }, [subscribeToConversation]);

  const disconnect = useCallback(() => {
    clientRef.current?.deactivate();
    clientRef.current = null;
    setConnected(false);
  }, []);

  const sendMessage = useCallback((message: string, convId: string) => {
    if (!clientRef.current?.active) return;
    clientRef.current.publish({
      destination: '/app/chat',
      body: JSON.stringify({ message, conversationId: convId }),
    });
  }, []);

  // Réabonnement si le conversationId change alors que le client est déjà connecté
  useEffect(() => {
    if (clientRef.current?.active && connected) {
      subscribeToConversation(clientRef.current, conversationId);
    }
  }, [conversationId, connected, subscribeToConversation]);

  useEffect(() => {
    return () => {
      clientRef.current?.deactivate();
    };
  }, []);

  return { connected, lastEvent, connect, disconnect, sendMessage };
}
