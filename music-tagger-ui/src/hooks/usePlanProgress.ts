import { useCallback, useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { environment } from '../config/environment';
import type { TagProgressEvent } from '../types/types';

/**
 * Hook de souscription WebSocket STOMP aux événements de progression d'un plan.
 * Se connecte à /topic/plan/{planId}/progress et accumule les TagProgressEvent reçus.
 * Se déconnecte automatiquement au démontage ou changement de planId.
 */
export function usePlanProgress(planId: string | null) {
  const clientRef = useRef<Client | null>(null);
  const [events, setEvents] = useState<TagProgressEvent[]>([]);
  const [connected, setConnected] = useState(false);

  const disconnect = useCallback(() => {
    clientRef.current?.deactivate();
    clientRef.current = null;
    setConnected(false);
  }, []);

  useEffect(() => {
    if (!planId) return;

    setEvents([]);

    const client = new Client({
      webSocketFactory: () => new SockJS(environment.wsUrl),
      reconnectDelay: 5000,
      onConnect: () => {
        setConnected(true);
        client.subscribe(`/topic/plan/${planId}/progress`, (frame) => {
          const event: TagProgressEvent = JSON.parse(frame.body);
          setEvents((prev) => [...prev, event]);
        });
      },
      onDisconnect: () => setConnected(false),
      onStompError: () => setConnected(false),
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      clientRef.current = null;
      setConnected(false);
    };
  }, [planId]);

  return { events, connected, disconnect };
}
