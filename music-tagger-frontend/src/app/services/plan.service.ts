import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { TaggingPlan, TagPreview, BatchApplyResult, TaggingHistoryEntry } from '../models/types';

@Injectable({ providedIn: 'root' })
export class PlanService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/api/plan`;

  create(filePaths: string[]): Observable<TaggingPlan> {
    return this.http.post<TaggingPlan>(`${this.baseUrl}/create`, filePaths);
  }

  get(planId: string): Observable<TaggingPlan> {
    return this.http.get<TaggingPlan>(`${this.baseUrl}/${planId}`);
  }

  approve(planId: string): Observable<TaggingPlan> {
    return this.http.put<TaggingPlan>(`${this.baseUrl}/${planId}/approve`, null);
  }

  execute(planId: string): Observable<BatchApplyResult> {
    return this.http.post<BatchApplyResult>(`${this.baseUrl}/${planId}/execute`, null);
  }

  preview(planId: string): Observable<TagPreview[]> {
    return this.http.get<TagPreview[]>(`${this.baseUrl}/${planId}/preview`);
  }

  getHistory(planId: string): Observable<TaggingHistoryEntry[]> {
    return this.http.get<TaggingHistoryEntry[]>(`${this.baseUrl}/${planId}/history`);
  }

  delete(planId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${planId}`);
  }
}
