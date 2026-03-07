import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ChatMessage, ChatRequest, ChatResponse } from '../models/types';

@Injectable({ providedIn: 'root' })
export class AgentService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/api/agent`;

  chat(message: string, conversationId?: string): Observable<ChatResponse> {
    const request: ChatRequest = { message, conversationId };
    return this.http.post<ChatResponse>(`${this.baseUrl}/chat`, request);
  }

  getHistory(conversationId: string): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`${this.baseUrl}/conversations/${conversationId}/history`);
  }

  deleteConversation(conversationId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/conversations/${conversationId}`);
  }
}
