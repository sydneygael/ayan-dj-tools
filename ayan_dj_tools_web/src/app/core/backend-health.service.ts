import { Injectable, inject, signal } from '@angular/core';
import { catchError, interval, of, startWith, switchMap } from 'rxjs';
import { ApiService } from './api.service';

export type BackendStatus = 'up' | 'down' | 'checking';

@Injectable({ providedIn: 'root' })
export class BackendHealthService {
  private readonly api = inject(ApiService);

  readonly status = signal<BackendStatus>('checking');
  readonly lastError = signal<string | null>(null);

  constructor() {
    interval(10000)
      .pipe(
        startWith(0), // déclenche un check immédiat sans attendre la première tick de 10 s

        switchMap(() =>
          this.api.getHealth().pipe(
            catchError((error: unknown) => {
              const message = error instanceof Error ? error.message : 'Backend inaccessible';
              this.status.set('down');
              this.lastError.set(message);
              return of(null);
            })
          )
        )
      )
      .subscribe((health) => {
        if (!health) {
          return;
        }
        this.status.set((health.status ?? '').toLowerCase() === 'up' ? 'up' : 'down');
        this.lastError.set(null);
      });
  }
}
