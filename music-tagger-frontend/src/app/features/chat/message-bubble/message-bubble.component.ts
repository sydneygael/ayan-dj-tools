import { Component, input } from '@angular/core';
import { ChatMessage } from '../../../models/types';

@Component({
  selector: 'app-message-bubble',
  standalone: true,
  template: `
    <div class="bubble" [class.user]="message().role === 'user'" [class.agent]="message().role === 'agent'">
      <div class="role">{{ message().role === 'user' ? 'Vous' : 'Ayan' }}</div>
      <div class="content">{{ message().content }}</div>
      <div class="time">{{ formatTime(message().timestamp) }}</div>
    </div>
  `,
  styles: `
    .bubble {
      max-width: 80%;
      padding: 10px 14px;
      border-radius: 12px;
      margin: 4px 0;
      font-size: 0.9rem;
      line-height: 1.5;
    }
    .user {
      background: var(--mat-sys-primary-container);
      color: var(--mat-sys-on-primary-container);
      align-self: flex-end;
      margin-left: auto;
      border-bottom-right-radius: 4px;
    }
    .agent {
      background: var(--mat-sys-surface-container-high);
      color: var(--mat-sys-on-surface);
      align-self: flex-start;
      border-bottom-left-radius: 4px;
    }
    .role {
      font-size: 0.7rem;
      font-weight: 500;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      opacity: 0.7;
      margin-bottom: 2px;
    }
    .content {
      white-space: pre-wrap;
      word-break: break-word;
    }
    .time {
      font-size: 0.65rem;
      opacity: 0.5;
      text-align: right;
      margin-top: 4px;
    }
  `,
})
export class MessageBubbleComponent {
  message = input.required<ChatMessage>();

  formatTime(timestamp: string): string {
    try {
      return new Date(timestamp).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
    } catch {
      return '';
    }
  }
}
