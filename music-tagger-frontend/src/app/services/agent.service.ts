import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ChatMessage, ChatRequest, ChatResponse } from '../models/types';

/**
 * Client HTTP pour l'agent IA Ayan (REST fallback).
 * Communique avec AgentController cote backend (POST /api/agent/...).
 */
@Injectable({ providedIn: 'root' })
export class AgentService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/api/agent`;

  /** Envoie un message a l'agent et recoit sa reponse. POST /api/agent/chat */
  chat(message: string, conversationId?: string): Observable<ChatResponse> {
    const request: ChatRequest = { message, conversationId };
    return this.http.post<ChatResponse>(`${this.baseUrl}/chat`, request);
  }

  /** Recupere l'historique d'une conversation. GET /api/agent/conversations/{id}/history */
  getHistory(conversationId: string): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`${this.baseUrl}/conversations/${conversationId}/history`);
  }

  /** Supprime une conversation et son historique Redis. DELETE /api/agent/conversations/{id} */
  deleteConversation(conversationId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/conversations/${conversationId}`);
  }
}
