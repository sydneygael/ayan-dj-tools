import { Injectable, inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { ModeService } from './mode.service';
import { FileSelectionService } from './file-selection.service';

/**
 * Service global de raccourcis clavier — écoute document:keydown dès l'injection.
 * Injecter dans App pour l'activer.
 */
@Injectable({ providedIn: 'root' })
export class KeyboardShortcutsService {
  private document = inject(DOCUMENT);
  private router = inject(Router);
  private modeService = inject(ModeService);
  private fileService = inject(FileSelectionService);
  private dialog = inject(MatDialog);

  private readonly listener = (event: KeyboardEvent) => this.onKeyDown(event);

  constructor() {
    this.document.addEventListener('keydown', this.listener);
  }

  private onKeyDown(event: KeyboardEvent): void {
    const target = event.target as HTMLElement;
    if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA') return;

    if (event.ctrlKey) {
      switch (event.key.toLowerCase()) {
        case 'p':
          event.preventDefault();
          this.modeService.setMode('PLAN');
          break;
        case 'm':
          event.preventDefault();
          this.modeService.setMode('MANUAL');
          break;
        case 'a':
          event.preventDefault();
          this.modeService.setMode('APPLY');
          break;
        case 'h':
          event.preventDefault();
          this.router.navigate(['/history']);
          break;
        case ',':
          event.preventDefault();
          this.router.navigate(['/settings']);
          break;
        case 's':
          event.preventDefault();
          this.router.navigate(['/stats']);
          break;
        case 'o':
          event.preventDefault();
          this.fileService.selectFiles();
          break;
      }
    } else if (event.key === '?') {
      import('../features/dialogs/shortcuts-help-dialog/shortcuts-help-dialog.component').then(m => {
        this.dialog.open(m.ShortcutsHelpDialogComponent);
      });
    }
  }
}
