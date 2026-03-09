import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { SimilarTrackResult } from '../models/types';

/**
 * Client HTTP pour la recherche semantique de pistes similaires (RAG / Qdrant).
 * Communique avec RagController cote backend (GET /api/rag/...).
 */
@Injectable({ providedIn: 'root' })
export class RagService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/api/rag`;

  /** Recherche des pistes similaires par requete textuelle. GET /api/rag/similar?query=&limit= */
  findSimilar(query: string, limit: number = 5): Observable<SimilarTrackResult[]> {
    return this.http.get<SimilarTrackResult[]>(`${this.baseUrl}/similar`, {
      params: { query, limit: limit.toString() },
    });
  }
}
