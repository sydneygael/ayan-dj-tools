import { Component, ElementRef, OnDestroy, OnInit, effect, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MessageBubbleComponent } from './message-bubble/message-bubble.component';
import { AgentService } from '../../services/agent.service';
import { WebSocketService } from '../../services/websocket.service';
import { ChatMessage } from '../../models/types';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MessageBubbleComponent,
  ],
  template: `
    <div class="chat-container">
      <div class="messages" #messagesContainer>
        @if (messages().length === 0) {
          <div class="welcome">
            <mat-icon class="welcome-icon">smart_toy</mat-icon>
            <h2>Salut ! Je suis Ayan</h2>
            <p>Ton assistant DJ pour gerer tes tags audio. Envoie-moi un message pour commencer.</p>
          </div>
        }
        @for (msg of messages(); track $index) {
          <app-message-bubble [message]="msg" />
        }
        @if (loading()) {
          <div class="loading">
            <mat-spinner diameter="24" />
            <span>Ayan reflechit...</span>
          </div>
        }
      </div>
      <div class="input-area">
        <mat-form-field class="message-input" appearance="outline">
          <input
            matInput
            [(ngModel)]="inputText"
            placeholder="Ecris un message a Ayan..."
            (keydown.enter)="send()"
            [disabled]="loading()"
          />
        </mat-form-field>
        <button mat-fab (click)="send()" [disabled]="!inputText.trim() || loading()" color="primary">
          <mat-icon>send</mat-icon>
        </button>
      </div>
    </div>
  `,
  styles: `
    .chat-container {
      display: flex;
      flex-direction: column;
      height: 100%;
    }
    .messages {
      flex: 1;
      overflow-y: auto;
      padding: 16px;
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
    .welcome {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 100%;
      text-align: center;
      color: var(--mat-sys-on-surface-variant);
      .welcome-icon {
        font-size: 64px;
        width: 64px;
        height: 64px;
        color: var(--mat-sys-primary);
        margin-bottom: 16px;
      }
      h2 { margin: 0 0 8px; }
      p { margin: 0; max-width: 400px; }
    }
    .loading {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 14px;
      color: var(--mat-sys-on-surface-variant);
      font-size: 0.85rem;
    }
    .input-area {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 16px 16px;
      border-top: 1px solid var(--mat-sys-outline-variant);
    }
    .message-input {
      flex: 1;
    }
  `,
})
/**
 * Composant principal de chat avec l'agent Ayan.
 * Utilise WebSocket STOMP en priorite, avec fallback REST si la connexion WS echoue.
 * Un effect() ecoute les messages entrants du WebSocket pour mettre a jour la conversation.
 */
export class ChatComponent implements OnInit, OnDestroy {
  private agentService = inject(AgentService);
  private wsService = inject(WebSocketService);
  private messagesContainer = viewChild<ElementRef>('messagesContainer');

  messages = signal<ChatMessage[]>([]);
  conversationId = signal<string | null>(null);
  loading = signal(false);
  inputText = '';

  constructor() {
    // Ecoute les reponses WebSocket pour ajouter les messages de l'agent a la conversation
    effect(() => {
      const response = this.wsService.lastMessage();
      if (response) {
        this.conversationId.set(response.conversationId);
        this.messages.update(msgs => [
          ...msgs,
          { role: 'agent', content: response.reply, timestamp: response.timestamp },
        ]);
        this.loading.set(false);
        this.scrollToBottom();
      }
    });
  }

  ngOnInit(): void {
    this.wsService.connect();
  }

  ngOnDestroy(): void {
    this.wsService.disconnect();
  }

  send(): void {
    const text = this.inputText.trim();
    if (!text || this.loading()) return;

    this.messages.update(msgs => [
      ...msgs,
      { role: 'user', content: text, timestamp: new Date().toISOString() },
    ]);
    this.inputText = '';
    this.loading.set(true);
    this.scrollToBottom();

    if (this.wsService.connected()) {
      // Envoi via WebSocket STOMP (temps reel)
      this.wsService.sendMessage(text, this.conversationId() ?? undefined);
    } else {
      // Fallback REST si WebSocket non connecte
      this.agentService.chat(text, this.conversationId() ?? undefined).subscribe({
        next: response => {
          this.conversationId.set(response.conversationId);
          this.messages.update(msgs => [
            ...msgs,
            { role: 'agent', content: response.reply, timestamp: response.timestamp },
          ]);
          this.loading.set(false);
          this.scrollToBottom();
        },
        error: () => {
          this.messages.update(msgs => [
            ...msgs,
            { role: 'agent', content: 'Erreur de connexion au serveur.', timestamp: new Date().toISOString() },
          ]);
          this.loading.set(false);
        },
      });
    }
  }

  /** Scroll automatique vers le bas apres ajout d'un message (setTimeout pour attendre le rendu). */
  private scrollToBottom(): void {
    setTimeout(() => {
      const el = this.messagesContainer()?.nativeElement;
      if (el) el.scrollTop = el.scrollHeight;
    });
  }
}
