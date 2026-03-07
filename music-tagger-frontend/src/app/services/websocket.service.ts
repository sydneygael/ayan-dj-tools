import { Injectable, signal } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { environment } from '../../environments/environment';
import { ChatResponse } from '../models/types';

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
