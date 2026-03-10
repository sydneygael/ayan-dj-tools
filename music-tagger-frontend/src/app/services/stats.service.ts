import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { StatsReport } from '../models/types';

@Injectable({ providedIn: 'root' })
export class StatsService {
  private http = inject(HttpClient);

  getStats(): Observable<StatsReport> {
    return this.http.get<StatsReport>(`${environment.apiUrl}/api/stats`);
  }
}
