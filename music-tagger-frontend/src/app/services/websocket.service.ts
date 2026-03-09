import { Injectable, signal } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { environment } from '../../environments/environment';
import { ChatResponse } from '../models/types';

/**
 * Service de communication WebSocket STOMP avec le backend.
 * Se connecte via SockJS sur /ws, s'abonne a /topic/responses pour recevoir
 * les reponses de l'agent, et publie les messages sur /app/chat.
 * Reconnexion automatique toutes les 5 secondes en cas de deconnexion.
 */
@Injectable({ providedIn: 'root' })
export class WebSocketService {
  private client: Client | null = null;
  readonly connected = signal(false);
  readonly lastMessage = signal<ChatResponse | null>(null);

  connect(): void {
    if (this.client?.connected) return;

    this.client = new Client({
      webSocketFactory: () => new SockJS(environment.wsUrl) as WebSocket,
      reconnectDelay: 5000,
      onConnect: () => {
        this.connected.set(true);
        this.client!.subscribe('/topic/responses', (message: IMessage) => {
          const response: ChatResponse = JSON.parse(message.body);
          this.lastMessage.set(response);
        });
      },
      onDisconnect: () => this.connected.set(false),
      onStompError: () => this.connected.set(false),
    });

    this.client.activate();
  }

  /** Publie un message utilisateur sur /app/chat via STOMP. */
  sendMessage(message: string, conversationId?: string): void {
    if (!this.client?.connected) return;
    this.client.publish({
      destination: '/app/chat',
      body: JSON.stringify({ message, conversationId }),
    });
  }

  disconnect(): void {
    this.client?.deactivate();
    this.connected.set(false);
  }
}
