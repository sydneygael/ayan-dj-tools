import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { BatchApplyResult, TagPreview } from '../models/types';

@Injectable({ providedIn: 'root' })
export class TagsService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/api/tags`;

  apply(operations: Record<string, Record<string, string>>): Observable<BatchApplyResult> {
    return this.http.post<BatchApplyResult>(`${this.baseUrl}/apply`, operations);
  }

  preview(filepath: string, tags: Record<string, string>): Observable<TagPreview> {
    return this.http.post<TagPreview>(`${this.baseUrl}/preview`, { filepath, tags });
  }
}
