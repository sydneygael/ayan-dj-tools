import { Injectable, signal } from '@angular/core';

/** Gestion du thème dark/light — persiste dans localStorage. */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  readonly isDark = signal(localStorage.getItem('theme') !== 'light');

  /** Applique le thème stocké au démarrage. */
  apply(): void {
    if (this.isDark()) {
      document.body.classList.remove('light-theme');
    } else {
      document.body.classList.add('light-theme');
    }
  }

  /** Bascule entre dark et light. */
  toggle(): void {
    this.isDark.update(d => !d);
    localStorage.setItem('theme', this.isDark() ? 'dark' : 'light');
    this.apply();
  }
}
