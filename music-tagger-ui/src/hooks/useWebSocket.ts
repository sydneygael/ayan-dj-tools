import { useCallback, useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { environment } from '../config/environment';
import type { ChatResponse } from '../types/types';

/**
 * Hook de connexion WebSocket STOMP vers le backend.
 * Utilise SockJS comme transport et @stomp/stompjs comme client STOMP.
 * Fournit : connect(), disconnect(), sendMessage(), connected (état), lastMessage (dernière réponse).
 * La reconnexion automatique est configurée à 5 secondes.
 * Le client s'abonne au topic /topic/responses pour recevoir les réponses de l'agent.
 */
export function useWebSocket() {
  const clientRef = useRef<Client | null>(null);
  const [connected, setConnected] = useState(false);
  const [lastMessage, setLastMessage] = useState<ChatResponse | null>(null);

  const connect = useCallback(() => {
    if (clientRef.current?.active) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(environment.wsUrl),
      reconnectDelay: 5000,
      onConnect: () => {
        setConnected(true);
        client.subscribe('/topic/responses', (frame) => {
          const response: ChatResponse = JSON.parse(frame.body);
          setLastMessage(response);
        });
      },
      onDisconnect: () => setConnected(false),
      onStompError: () => setConnected(false),
    });

    client.activate();
    clientRef.current = client;
  }, []);

  const disconnect = useCallback(() => {
    clientRef.current?.deactivate();
    clientRef.current = null;
    setConnected(false);
  }, []);

  const sendMessage = useCallback((message: string, conversationId?: string) => {
    if (!clientRef.current?.active) return;
    clientRef.current.publish({
      destination: '/app/chat',
      body: JSON.stringify({ message, conversationId }),
    });
  }, []);

  // Cleanup de sécurité : désactive le client STOMP au démontage du hook,
  // même si disconnect() n'a pas été appelé explicitement par le composant parent.
  useEffect(() => {
    return () => {
      clientRef.current?.deactivate();
    };
  }, []);

  return { connected, lastMessage, connect, disconnect, sendMessage };
}
