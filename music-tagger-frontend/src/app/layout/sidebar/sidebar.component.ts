import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { FileSelectionService } from '../../services/file-selection.service';
import { PlanService } from '../../services/plan.service';
import { FileListComponent } from '../../features/file-list/file-list.component';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, MatDividerModule, FileListComponent],
  template: `
    <div class="sidebar">
      <div class="sidebar-header">
        <h3>Fichiers</h3>
        <button mat-icon-button (click)="fileService.clearFiles()" [disabled]="fileService.selectedFiles().length === 0">
          <mat-icon>delete_sweep</mat-icon>
        </button>
      </div>
      <button mat-flat-button (click)="fileService.selectFiles()" class="select-btn">
        <mat-icon>folder_open</mat-icon>
        Selectionner des fichiers
      </button>
      @if (!fileService.isElectron()) {
        <p class="hint">Mode navigateur : file picker disponible uniquement en mode Electron</p>
      }
      @if (fileService.selectedFiles().length > 0) {
        <button mat-stroked-button (click)="createPlan()" class="plan-btn" [disabled]="creatingPlan">
          <mat-icon>assignment</mat-icon>
          Creer un plan
        </button>
      }
      <mat-divider />
      <app-file-list [files]="fileService.selectedFiles()" (fileRemoved)="fileService.removeFile($event)" />
    </div>
  `,
  styles: `
    .sidebar {
      display: flex;
      flex-direction: column;
      height: 100%;
      padding: 12px;
      gap: 8px;
      overflow-y: auto;
    }
    .sidebar-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      h3 {
        margin: 0;
        font-size: 0.9rem;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        color: var(--mat-sys-on-surface-variant);
      }
    }
    .select-btn {
      width: 100%;
    }
    .plan-btn {
      width: 100%;
    }
    .hint {
      font-size: 0.75rem;
      color: var(--mat-sys-on-surface-variant);
      margin: 0;
    }
  `,
})
/**
 * Sidebar de l'application — selection de fichiers audio, liste des fichiers selectionnes,
 * et bouton « Creer un plan » qui appelle le backend puis navigue vers la page de revue.
 */
export class SidebarComponent {
  protected fileService = inject(FileSelectionService);
  private planService = inject(PlanService);
  private router = inject(Router);
  protected creatingPlan = false;

  /** Cree un plan via le backend et navigue vers /plan/:id pour la revue. */
  createPlan(): void {
    this.creatingPlan = true;
    this.planService.create(this.fileService.selectedFiles()).subscribe({
      next: plan => {
        this.creatingPlan = false;
        this.router.navigate(['/plan', plan.planId]);
      },
      error: () => {
        this.creatingPlan = false;
      },
    });
  }
}
