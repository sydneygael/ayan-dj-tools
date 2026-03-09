import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { TaggingPlan, TagPreview, BatchApplyResult, TaggingHistoryEntry } from '../models/types';

/**
 * Client HTTP pour les operations sur les plans de tagging.
 * Communique avec PlanController cote backend (POST /api/plan/...).
 */
@Injectable({ providedIn: 'root' })
export class PlanService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/api/plan`;

  /** Cree un plan a partir d'une liste de chemins de fichiers audio. POST /api/plan/create */
  create(filePaths: string[]): Observable<TaggingPlan> {
    return this.http.post<TaggingPlan>(`${this.baseUrl}/create`, filePaths);
  }

  get(planId: string): Observable<TaggingPlan> {
    return this.http.get<TaggingPlan>(`${this.baseUrl}/${planId}`);
  }

  /** Approuve toutes les operations du plan. PUT /api/plan/{id}/approve */
  approve(planId: string): Observable<TaggingPlan> {
    return this.http.put<TaggingPlan>(`${this.baseUrl}/${planId}/approve`, null);
  }

  /** Lance l'execution du plan (ecriture des tags). POST /api/plan/{id}/execute */
  execute(planId: string): Observable<BatchApplyResult> {
    return this.http.post<BatchApplyResult>(`${this.baseUrl}/${planId}/execute`, null);
  }

  /** Recupere un apercu des changements avant execution. GET /api/plan/{id}/preview */
  preview(planId: string): Observable<TagPreview[]> {
    return this.http.get<TagPreview[]>(`${this.baseUrl}/${planId}/preview`);
  }

  /** Recupere l'historique des modifications appliquees pour un plan. GET /api/plan/{id}/history */
  getHistory(planId: string): Observable<TaggingHistoryEntry[]> {
    return this.http.get<TaggingHistoryEntry[]>(`${this.baseUrl}/${planId}/history`);
  }

  /** Supprime un plan. DELETE /api/plan/{id} */
  delete(planId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${planId}`);
  }
}
