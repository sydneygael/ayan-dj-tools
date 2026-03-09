import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { BatchApplyResult, TagPreview } from '../models/types';

/**
 * Client HTTP pour les operations directes sur les tags audio.
 * Communique avec TagController cote backend (POST /api/tags/...).
 */
@Injectable({ providedIn: 'root' })
export class TagsService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/api/tags`;

  /** Applique des tags sur plusieurs fichiers. POST /api/tags/apply — cle = filepath, valeur = tags */
  apply(operations: Record<string, Record<string, string>>): Observable<BatchApplyResult> {
    return this.http.post<BatchApplyResult>(`${this.baseUrl}/apply`, operations);
  }

  /** Previsualise les changements de tags sans les appliquer. POST /api/tags/preview */
  preview(filepath: string, tags: Record<string, string>): Observable<TagPreview> {
    return this.http.post<TagPreview>(`${this.baseUrl}/preview`, { filepath, tags });
  }
}
