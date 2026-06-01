import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  ActivityTimeline,
  BackendHealth,
  BatchApplyResult,
  ChatMessage,
  ChatRequest,
  ChatResponse,
  ChatStreamEvent,
  CollectionProfile,
  EnrichmentStats,
  FileAnalysisItem,
  FileBrowserPage,
  FileEnrichItem,
  HarmonicPlaylist,
  OperatingMode,
  PlanProgressResponse,
  Playlist,
  SimilarTrackResult,
  StatsReport,
  TagOperation,
  TagPreview,
  TaggingHistoryEntry,
  TaggingPlan
} from './models';
import { PreferencesStore } from './preferences.store';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly prefs = inject(PreferencesStore);

  getHealth(): Observable<BackendHealth> {
    return this.http.get<BackendHealth>(this.url('/actuator/health'));
  }

  chat(request: ChatRequest): Observable<ChatResponse> {
    return this.http.post<ChatResponse>(this.url('/api/agent/chat'), request);
  }

  chatStream(request: ChatRequest): Observable<ChatStreamEvent> {
    return new Observable<ChatStreamEvent>((subscriber) => {
      const controller = new AbortController();
      const url = this.url('/api/agent/chat/stream');

      (async () => {
        try {
          const response = await fetch(url, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              Accept: 'text/event-stream'
            },
            body: JSON.stringify(request),
            signal: controller.signal
          });

          if (!response.ok || !response.body) {
            subscriber.error(new Error(`HTTP ${response.status} ${response.statusText}`));
            return;
          }

          const reader = response.body.getReader();
          const decoder = new TextDecoder();
          let buffer = '';

          while (true) {
            const { value, done } = await reader.read();
            if (done) break;
            buffer += decoder.decode(value, { stream: true });

            let frameEnd;
            while ((frameEnd = buffer.indexOf('\n\n')) !== -1) {
              const frame = buffer.slice(0, frameEnd);
              buffer = buffer.slice(frameEnd + 2);
              const dataLine = frame
                .split('\n')
                .find((line) => line.startsWith('data:'));
              if (!dataLine) continue;
              const payload = dataLine.slice(5).trim();
              if (!payload) continue;
              try {
                subscriber.next(JSON.parse(payload) as ChatStreamEvent);
              } catch (err) {
                subscriber.error(err);
                return;
              }
            }
          }
          subscriber.complete();
        } catch (err) {
          if ((err as { name?: string }).name !== 'AbortError') {
            subscriber.error(err);
          }
        }
      })();

      return () => controller.abort();
    });
  }

  getConversationHistory(id: string): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(this.url(`/api/agent/conversations/${id}/history`));
  }

  createPlan(filePaths: string[], mode: OperatingMode): Observable<TaggingPlan> {
    return this.http.post<TaggingPlan>(this.url('/api/plan/create'), { filePaths, mode });
  }

  getPlan(planId: string): Observable<TaggingPlan> {
    return this.http.get<TaggingPlan>(this.url(`/api/plan/${planId}`));
  }

  getPlanProgress(planId: string): Observable<PlanProgressResponse> {
    return this.http.get<PlanProgressResponse>(this.url(`/api/plan/${planId}/progress`));
  }

  approvePlan(planId: string): Observable<TaggingPlan> {
    return this.http.put<TaggingPlan>(this.url(`/api/plan/${planId}/approve`), {});
  }

  executePlan(planId: string): Observable<BatchApplyResult> {
    return this.http.post<BatchApplyResult>(this.url(`/api/plan/${planId}/execute`), {});
  }

  autoExecutePlan(planId: string): Observable<void> {
    return this.http.post<void>(this.url(`/api/plan/${planId}/auto-execute`), {});
  }

  getCurrentOperation(planId: string): Observable<TagOperation> {
    return this.http.get<TagOperation>(this.url(`/api/plan/${planId}/current`));
  }

  confirmOperation(planId: string, index: number, approved: boolean): Observable<TagOperation> {
    const params = new HttpParams().set('approved', approved);
    return this.http.post<TagOperation>(this.url(`/api/plan/${planId}/operations/${index}/confirm`), {}, { params });
  }

  getPlanHistory(planId: string): Observable<TaggingHistoryEntry[]> {
    return this.http.get<TaggingHistoryEntry[]>(this.url(`/api/plan/${planId}/history`));
  }

  getPlanPreview(planId: string): Observable<TagPreview[]> {
    return this.http.get<TagPreview[]>(this.url(`/api/plan/${planId}/preview`));
  }

  generatePlaylist(bpmMin: number, bpmMax: number, genre: string): Observable<Playlist> {
    return this.http.post<Playlist>(this.url('/api/playlist/generate'), { bpmMin, bpmMax, genre });
  }

  generateHarmonicPlaylist(
    bpmMin: number,
    bpmMax: number,
    genre: string,
    targetEnergy: number,
    count: number
  ): Observable<HarmonicPlaylist> {
    return this.http.post<HarmonicPlaylist>(this.url('/api/playlist/generate-harmonic'), {
      bpmMin,
      bpmMax,
      genre,
      targetEnergy,
      count
    });
  }

  findSimilarTracks(query: string, limit = 5): Observable<SimilarTrackResult[]> {
    const params = new HttpParams().set('query', query).set('limit', limit);
    return this.http.get<SimilarTrackResult[]>(this.url('/api/rag/similar'), { params });
  }

  getStats(): Observable<StatsReport> {
    return this.http.get<StatsReport>(this.url('/api/stats'));
  }

  getCollectionProfile(): Observable<CollectionProfile> {
    return this.http.get<CollectionProfile>(this.url('/api/stats/collection'));
  }

  getEnrichmentStats(): Observable<EnrichmentStats> {
    return this.http.get<EnrichmentStats>(this.url('/api/stats/enrichment'));
  }

  getActivityTimeline(period: string): Observable<ActivityTimeline> {
    const params = new HttpParams().set('period', period);
    return this.http.get<ActivityTimeline>(this.url('/api/stats/activity'), { params });
  }

  browsePath(path: string, page = 0, size = 20): Observable<FileBrowserPage> {
    const params = new HttpParams().set('path', path).set('page', String(page)).set('size', String(size));
    return this.http.get<FileBrowserPage>(this.url('/api/files/browse'), { params });
  }

  analyzeFiles(filePaths: string[]): Observable<FileAnalysisItem[]> {
    return this.http.post<FileAnalysisItem[]>(this.url('/api/files/analyze'), { filePaths });
  }

  enrichFiles(filePaths: string[]): Observable<FileEnrichItem[]> {
    return this.http.post<FileEnrichItem[]>(this.url('/api/files/enrich'), { filePaths });
  }

  private url(path: string): string {
    const base = this.prefs.apiBaseUrl().replace(/\/+$/, '');
    return `${base}${path}`;
  }
}
