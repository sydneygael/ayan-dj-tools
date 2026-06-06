import { Component, computed, inject, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { PreferencesStore } from '../core/preferences.store';

export interface QuickAction {
  label: string;
  message: string;
  needsFiles: boolean;
  hint?: string;
  direct?: 'analyze' | 'enrich';
}

const ACTIONS: QuickAction[] = [
  {
    label: 'Analyser sélection',
    message: 'Analyse les fichiers sélectionnés et liste les tags manquants pour chacun.',
    needsFiles: true,
    hint: 'scanMusicFile + detectMissingTags',
    direct: 'analyze'
  },
  {
    label: 'Enrichir via Soundcharts',
    message: 'Enrichis les fichiers sélectionnés via Soundcharts et résume les correspondances trouvées.',
    needsFiles: true,
    hint: 'enrichWithSpotify',
    direct: 'enrich'
  },
  {
    label: 'Suggestions intelligentes',
    message: 'Pour chaque fichier sélectionné, propose des tags intelligents avec niveau de confiance.',
    needsFiles: true,
    hint: 'smartSuggestTags'
  },
  {
    label: 'Créer un plan',
    message: 'Crée un plan de tagging pour les fichiers sélectionnés.',
    needsFiles: true,
    hint: 'createPlanForFiles'
  },
  {
    label: 'Trouver similaires',
    message: 'Trouve dans la collection des morceaux similaires au premier fichier sélectionné.',
    needsFiles: true,
    hint: 'findSimilarTracks'
  },
  {
    label: 'Historique récent',
    message: "Montre l'historique des dernières modifications appliquées.",
    needsFiles: false,
    hint: 'getTaggingHistory'
  }
];

@Component({
  standalone: true,
  selector: 'app-chat-quick-actions',
  imports: [MatButtonModule, MatTooltipModule],
  template: `
    <div class="quick-actions">
      @for (action of actions; track action.label) {
        <button
          mat-stroked-button
          class="action-chip"
          [disabled]="isDisabled(action)"
          [matTooltip]="isDisabled(action) ? 'Sélectionne des fichiers dabord' : (action.hint ?? '')"
          matTooltipShowDelay="400"
          (click)="trigger(action)"
        >
          {{ action.label }}
        </button>
      }
    </div>
  `,
  styles: `
    .quick-actions {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      margin-bottom: 8px;
    }

    .action-chip {
      font-size: .78rem !important;
      height: 28px !important;
      line-height: 28px !important;
      padding: 0 10px !important;
      border-radius: 999px !important;
      min-width: unset !important;
    }
  `
})
export class ChatQuickActionsComponent {
  private readonly prefs = inject(PreferencesStore);
  readonly actions = ACTIONS;
  readonly hasSelection = computed(() => this.prefs.selectedFileCount() > 0);
  readonly fire = output<QuickAction>();

  isDisabled(action: QuickAction): boolean {
    return action.needsFiles && !this.hasSelection();
  }

  trigger(action: QuickAction): void {
    if (this.isDisabled(action)) return;
    this.fire.emit(action);
  }
}
