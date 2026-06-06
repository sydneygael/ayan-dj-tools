import { CommonModule } from '@angular/common';
import { Component, ElementRef, ViewChild, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { ApiService } from '../core/api.service';
import { ChatRequest, FileAnalysisItem, FileEnrichItem, OperatingMode, UiMessage } from '../core/models';
import { PreferencesStore } from '../core/preferences.store';
import { ChatQuickActionsComponent, QuickAction } from '../shared/chat-quick-actions.component';
import { ChatToolCallCardComponent } from '../shared/chat-tool-call-card.component';
import { FilePickerComponent } from '../shared/file-picker.component';

// Détecte les intentions qui nécessitent une sélection de fichiers pour bloquer l'envoi à vide.
const INTENT_RX = /\b(analys|tagu|enrichi|plan|applique|scan|simil)/i;

@Component({
  standalone: true,
  selector: 'app-chat-page',
  imports: [
    CommonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatSnackBarModule,
    FilePickerComponent,
    ChatQuickActionsComponent,
    ChatToolCallCardComponent,
  ],
  template: `
    <div class="page-header">
      <h1>Chat</h1>
      <div class="header-right">
        <span class="file-badge">{{ prefs.selectedFileCount() }} fichier(s)</span>

        <mat-form-field subscriptSizing="dynamic" class="mode-field">
          <mat-label>Mode</mat-label>
          <mat-select [value]="prefs.currentMode()" (selectionChange)="onModeChange($event.value)">
            <mat-option value="PLAN">PLAN</mat-option>
            <mat-option value="MANUAL">MANUAL</mat-option>
            <mat-option value="APPLY">APPLY</mat-option>
          </mat-select>
        </mat-form-field>
      </div>
    </div>

    <p class="hint">Conseille Ayan et crée un plan de tagging.</p>

    <div class="chat-grid">
      <!-- Conversation -->
      <mat-card class="conversation-card">
        <mat-card-header>
          <mat-card-title>Conversation</mat-card-title>
          <mat-card-subtitle>{{ conversationId().slice(0, 8) }}…</mat-card-subtitle>
        </mat-card-header>

        <mat-card-content>
          <div class="messages" #messagesContainer>
            @if (messages().length === 0) {
              <div class="empty-messages">
                <mat-icon fontSet="material-symbols-rounded">chat_bubble</mat-icon>
                <span>Aucun message pour l'instant.</span>
              </div>
            } @else {
              @for (msg of messages(); track $index) {
                @switch (msg.kind) {
                  @case ('text') {
                    <div class="msg" [class.user]="msg.role === 'user'">
                      <div class="msg-role">{{ msg.role === 'user' ? 'Vous' : 'Ayan' }}</div>
                      <div class="msg-content">
                        {{ msg.content }}
                        @if (msg.role === 'assistant' && msg.content === '' && isSending()) {
                          <span class="thinking-dots">···</span>
                        }
                      </div>
                    </div>
                  }
                  @case ('status') {
                    <div class="status-msg"
                         [class.running]="msg.state === 'running'"
                         [class.done]="msg.state === 'done'"
                         [class.error]="msg.state === 'error'">
                      <mat-icon fontSet="material-symbols-rounded"
                                class="status-icon"
                                [class.spin]="msg.state === 'running'">
                        {{ statusIcon(msg.state) }}
                      </mat-icon>
                      <div class="status-content">
                        <div class="status-title">{{ msg.title }}</div>
                        <div class="status-detail">{{ msg.detail }}</div>
                      </div>
                    </div>
                  }
                  @case ('tool') {
                    <app-chat-tool-call-card
                      [name]="msg.name"
                      [args]="msg.argsJson"
                      [status]="msg.status"
                      [result]="msg.resultJson"
                    />
                  }
                }
              }
            }
          </div>
        </mat-card-content>

        <mat-card-content class="quick-actions-wrap">
          <app-chat-quick-actions (fire)="onQuickAction($event)" />
        </mat-card-content>

        <mat-card-actions class="input-area">
          @if (guardError(); as g) {
            <p class="warn-hint">
              <mat-icon fontSet="material-symbols-rounded" class="warn-icon">warning</mat-icon>
              {{ g }}
            </p>
          }
          <div class="input-row">
            <mat-form-field class="message-field" subscriptSizing="dynamic">
              <input
                matInput
                [value]="messageInput()"
                (input)="messageInput.set($any($event.target).value)"
                placeholder="Ex : analyse les fichiers sélectionnés…"
                (keyup.enter)="sendMessage()"
              />
            </mat-form-field>
            <button mat-flat-button
              (click)="sendMessage()"
              [disabled]="!canSend()"
              matTooltip="Envoyer (Entrée)">
              <mat-icon fontSet="material-symbols-rounded"
                        [class.spin]="isSending()">
                {{ isSending() ? 'progress_activity' : 'send' }}
              </mat-icon>
            </button>
          </div>

          <div class="action-row">
            <button mat-button (click)="loadHistory()">
              <mat-icon fontSet="material-symbols-rounded">history</mat-icon>
              Historique
            </button>
            <button mat-button (click)="newConversation()">
              <mat-icon fontSet="material-symbols-rounded">add_comment</mat-icon>
              Nouvelle conversation
            </button>
          </div>

          @if (chatError()) {
            <p class="error">{{ chatError() }}</p>
            @if (lastFailedRequest()) {
              <button mat-stroked-button (click)="retry()">
                <mat-icon fontSet="material-symbols-rounded">refresh</mat-icon>
                Réessayer
              </button>
            }
          }
        </mat-card-actions>
      </mat-card>

      <!-- Plan creation -->
      <mat-card class="plan-card">
        <mat-card-header>
          <mat-card-title>Créer un plan</mat-card-title>
        </mat-card-header>

        <mat-card-content>
          <app-file-picker (filesSelected)="onFilesConfirmed($event)" />

          @if (prefs.selectedFileCount() > 0) {
            <p class="hint" style="margin-top: 10px">
              {{ prefs.selectedFileCount() }} fichier(s) prêt(s) — mode {{ prefs.currentMode() }}.
            </p>
          }
        </mat-card-content>

        @if (prefs.selectedFileCount() > 0) {
          <mat-card-actions>
            <button mat-flat-button
              (click)="createPlan()"
              [disabled]="isCreatingPlan()">
              <mat-icon fontSet="material-symbols-rounded"
                        [class.spin]="isCreatingPlan()">
                {{ isCreatingPlan() ? 'progress_activity' : 'task_alt' }}
              </mat-icon>
              Créer un plan ({{ prefs.selectedFileCount() }})
            </button>
          </mat-card-actions>
        }

        @if (planError()) {
          <mat-card-content>
            <p class="error">{{ planError() }}</p>
          </mat-card-content>
        }
      </mat-card>
    </div>
  `,
  styles: `
    .page-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      margin-bottom: 4px;
    }

    .page-header h1 { margin: 0; }

    .header-right {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .file-badge {
      font-size: .8rem;
      padding: 3px 10px;
      border-radius: 999px;
      background: var(--surface-2);
      color: var(--muted);
      white-space: nowrap;
    }

    .mode-field { width: 130px; }

    .chat-grid {
      display: grid;
      grid-template-columns: 1.35fr 1fr;
      gap: 16px;
      margin-top: 16px;
    }

    .conversation-card, .plan-card {
      display: flex;
      flex-direction: column;
    }

    .messages {
      min-height: 300px;
      max-height: 440px;
      overflow-y: auto;
      border: 1px solid var(--border);
      border-radius: 8px;
      padding: 8px;
      background: var(--surface-0);
    }

    .empty-messages {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 8px;
      height: 180px;
      color: var(--muted);
      font-size: .9rem;

      mat-icon { font-size: 36px; width: 36px; height: 36px; opacity: .4; }
    }

    .msg {
      margin: 6px 0;
      padding: 8px 10px;
      border-radius: 8px;
      background: var(--surface-2);
    }

    .msg.user {
      background: var(--accent-soft);
      margin-left: 20%;
    }

    .msg-role {
      font-size: .75rem;
      color: var(--muted);
      margin-bottom: 3px;
      text-transform: uppercase;
      letter-spacing: .04em;
    }

    .msg-content { white-space: pre-wrap; }

    .status-msg {
      margin: 6px 0;
      padding: 8px 10px;
      border-radius: 8px;
      border-left: 3px solid var(--accent-text);
      background: var(--surface-0);
      display: flex;
      gap: 8px;
      align-items: flex-start;
    }

    .status-msg.done {
      border-left-color: var(--success, #2a8);
    }

    .status-msg.error {
      border-left-color: var(--error, #f44336);
    }

    .status-icon {
      margin-top: 1px;
      font-size: 18px;
      width: 18px;
      height: 18px;
      color: var(--muted);
    }

    .status-content {
      display: grid;
      gap: 2px;
    }

    .status-title {
      font-size: .83rem;
      font-weight: 600;
    }

    .status-detail {
      font-size: .8rem;
      color: var(--muted);
    }

    .spin {
      animation: spin 1s linear infinite;
    }

    .thinking-dots {
      display: inline-block;
      color: var(--muted);
      font-size: 1.4rem;
      letter-spacing: .15em;
      animation: thinking-blink 1.2s steps(1) infinite;
    }

    @keyframes thinking-blink {
      0%, 100% { opacity: 1; }
      33%       { opacity: .4; }
      66%       { opacity: .1; }
    }

    @keyframes spin {
      from { transform: rotate(0deg); }
      to { transform: rotate(360deg); }
    }

    .quick-actions-wrap { padding-top: 0; }

    .input-area {
      display: flex;
      flex-direction: column;
      gap: 6px;
      padding: 8px 16px 16px;
    }

    .warn-hint {
      display: flex;
      align-items: center;
      gap: 6px;
      margin: 0;
      font-size: .83rem;
      color: #c8a000;
    }

    .warn-icon { font-size: 16px; width: 16px; height: 16px; color: #c8a000; }

    .input-row {
      display: flex;
      gap: 8px;
      align-items: flex-start;
    }

    .message-field { flex: 1; }

    .action-row {
      display: flex;
      gap: 4px;
      flex-wrap: wrap;
    }

    .error {
      margin: 4px 0;
      font-size: .85rem;
      color: var(--error, #f44336);
    }

    @media (max-width: 1100px) {
      .chat-grid { grid-template-columns: 1fr; }
    }
  `
})
export class ChatPageComponent {
  private readonly api = inject(ApiService);
  readonly prefs = inject(PreferencesStore);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  @ViewChild('messagesContainer') private messagesContainerRef?: ElementRef<HTMLElement>;

  readonly conversationId = signal<string>(crypto.randomUUID());
  readonly messages = signal<UiMessage[]>([]);
  readonly isSending = signal(false);
  readonly chatError = signal<string | null>(null);
  readonly messageInput = signal('');
  readonly lastFailedRequest = signal<ChatRequest | null>(null);

  readonly isCreatingPlan = signal(false);
  readonly planError = signal<string | null>(null);

  readonly guardError = computed<string | null>(() => {
    const text = this.messageInput().trim();
    if (!text) return null;
    if (this.prefs.selectedFileCount() === 0 && INTENT_RX.test(text)) {
      return "Cette demande nécessite une sélection. Sélectionne au moins un fichier dans le panneau de droite.";
    }
    return null;
  });

  readonly canSend = computed(() =>
    this.messageInput().trim().length > 0 && !this.guardError() && !this.isSending()
  );

  onModeChange(mode: OperatingMode): void {
    this.prefs.setCurrentMode(mode);
  }

  onQuickAction(action: QuickAction): void {
    if (action.direct === 'analyze') { void this.runDirectAnalyze(); return; }
    if (action.direct === 'enrich')  { void this.runDirectEnrich();  return; }
    this.sendTemplated(action.message);
  }

  async runDirectAnalyze(): Promise<void> {
    const paths = this.prefs.selectedFiles();
    this.addUserMessage('Analyse les fichiers sélectionnés et liste les tags manquants.');
    const idx = this.addPendingAssistant();
    const statusIdx = this.addStatusMessage(
      'Analyse des fichiers',
      'Préparation de la requête HTTP...',
      'running'
    );
    this.isSending.set(true);
    try {
      this.updateStatusMessage(statusIdx, 'Analyse des fichiers', 'Lecture des tags en cours...', 'running');
      const items = await firstValueFrom(this.api.analyzeFiles(paths));
      this.updateStatusMessage(statusIdx, 'Analyse des fichiers', 'Analyse terminée.', 'done');
      this.replaceAssistant(idx, this.formatAnalysis(items));
    } catch (err) {
      this.updateStatusMessage(statusIdx, 'Analyse des fichiers', this.errorToMessage(err), 'error');
      this.replaceAssistant(idx, '');
      this.chatError.set(this.errorToMessage(err));
    } finally {
      this.isSending.set(false);
      this.scrollMessagesToBottom();
    }
  }

  async runDirectEnrich(): Promise<void> {
    const paths = this.prefs.selectedFiles();
    this.addUserMessage('Enrichis les fichiers sélectionnés via Soundcharts.');
    const idx = this.addPendingAssistant();
    const statusIdx = this.addStatusMessage(
      'Enrichissement Soundcharts',
      'Préparation de la requête HTTP...',
      'running'
    );
    this.isSending.set(true);
    try {
      this.updateStatusMessage(statusIdx, 'Enrichissement Soundcharts', 'Recherche des métadonnées...', 'running');
      const items = await firstValueFrom(this.api.enrichFiles(paths));
      this.updateStatusMessage(statusIdx, 'Enrichissement Soundcharts', 'Enrichissement terminé.', 'done');
      this.replaceAssistant(idx, this.formatEnrichment(items));
    } catch (err) {
      this.updateStatusMessage(statusIdx, 'Enrichissement Soundcharts', this.errorToMessage(err), 'error');
      this.replaceAssistant(idx, '');
      this.chatError.set(this.errorToMessage(err));
    } finally {
      this.isSending.set(false);
      this.scrollMessagesToBottom();
    }
  }

  sendTemplated(message: string): void {
    this.messageInput.set(message);
    void this.sendMessage();
  }

  onFilesConfirmed(paths: string[]): void {
    this.snackBar.open(
      `${paths.length} fichier(s) prêt(s) — utilise les actions rapides ci-dessus.`,
      'OK',
      { duration: 4000, horizontalPosition: 'center', verticalPosition: 'top' }
    );
  }

  sendMessage(override?: ChatRequest): void {
    if (this.isSending()) return;

    const request: ChatRequest = override ?? {
      message: this.messageInput().trim(),
      conversationId: this.conversationId(),
      mode: this.prefs.currentMode(),
      filePaths: this.prefs.selectedFiles(),
      currentDir: this.prefs.currentDir()
    };

    if (!request.message || (!override && (this.guardError() || !request.message))) return;

    this.chatError.set(null);
    this.lastFailedRequest.set(null);

    if (!override) {
      this.messages.update((all) => [...all, { kind: 'text', role: 'user', content: request.message }]);
      this.messageInput.set('');
    }

    this.isSending.set(true);
    const idx = this.addPendingAssistant();
    const statusIdx = this.addStatusMessage('Chat', 'Étape 1/3 · Connexion au backend...', 'running');
    this.scrollMessagesToBottom();

    let accumulated = '';
    let streamStarted = false;

    this.api.chatStream(request).subscribe({
      next: (event) => {
        if (event.type === 'thinking' && !streamStarted) {
          this.updateStatusMessage(statusIdx, 'Chat', 'Étape 2/3 · Ayan réfléchit…', 'running');
        } else if (event.type === 'chunk' && event.token) {
          if (!streamStarted) {
            streamStarted = true;
            this.updateStatusMessage(statusIdx, 'Chat', 'Étape 2/3 · Réponse en cours de génération...', 'running');
          }
          accumulated += event.token;
          this.replaceAssistant(idx, accumulated);
          this.scrollMessagesToBottom();
        } else if (event.type === 'done') {
          if (event.conversationId) this.conversationId.set(event.conversationId);
          this.updateStatusMessage(statusIdx, 'Chat', 'Étape 3/3 · Réponse terminée.', 'done');
          this.replaceAssistant(idx, event.reply ?? accumulated);
          this.isSending.set(false);
          this.scrollMessagesToBottom();
        } else if (event.type === 'error') {
          this.updateStatusMessage(statusIdx, 'Chat', event.reply ?? 'Erreur du serveur', 'error');
          this.replaceAssistant(idx, '');
          this.chatError.set(event.reply ?? 'Erreur du serveur');
          this.lastFailedRequest.set(request);
          this.isSending.set(false);
        }
      },
      error: (err) => {
        this.updateStatusMessage(statusIdx, 'Chat', this.errorToMessage(err), 'error');
        this.replaceAssistant(idx, '');
        this.chatError.set(this.errorToMessage(err));
        this.lastFailedRequest.set(request);
        this.isSending.set(false);
      },
      complete: () => {
        // safety net: stream closed without done/error event
        if (this.isSending()) {
          this.updateStatusMessage(statusIdx, 'Chat', 'Étape 3/3 · Réponse terminée.', 'done');
          this.isSending.set(false);
        }
      }
    });
  }

  async retry(): Promise<void> {
    const req = this.lastFailedRequest();
    if (!req) return;
    this.chatError.set(null);
    this.sendMessage(req);
  }

  async loadHistory(): Promise<void> {
    this.chatError.set(null);
    try {
      const history = await firstValueFrom(this.api.getConversationHistory(this.conversationId()));
      this.messages.set(
        history.map((m) => ({
          kind: 'text' as const,
          role: m.role === 'assistant' ? 'assistant' : 'user',
          content: m.content
        }))
      );
    } catch (error: unknown) {
      this.chatError.set(this.errorToMessage(error));
    }
  }

  newConversation(): void {
    this.conversationId.set(crypto.randomUUID());
    this.messages.set([]);
    this.chatError.set(null);
    this.lastFailedRequest.set(null);
  }

  async createPlan(): Promise<void> {
    const filePaths = this.prefs.selectedFiles();
    if (filePaths.length === 0) {
      this.planError.set('Sélectionne au moins un fichier audio via le navigateur.');
      return;
    }

    this.addUserMessage('Crée un plan de tagging pour les fichiers sélectionnés.');
    const statusIdx = this.addStatusMessage('Création du plan', 'Étape 1/2 · Envoi de la demande...', 'running');
    this.planError.set(null);
    this.isCreatingPlan.set(true);
    try {
      const plan = await firstValueFrom(this.api.createPlan(filePaths, this.prefs.currentMode()));
      this.updateStatusMessage(
        statusIdx,
        'Création du plan',
        `Étape 2/2 · Plan créé (${plan.operations.length} opérations).`,
        'done'
      );
      await this.router.navigate(['/plan', plan.planId]);
    } catch (error: unknown) {
      this.updateStatusMessage(statusIdx, 'Création du plan', this.errorToMessage(error), 'error');
      this.planError.set(this.errorToMessage(error));
    } finally {
      this.isCreatingPlan.set(false);
    }
  }

  private addUserMessage(text: string): void {
    this.messages.update((all) => [...all, { kind: 'text', role: 'user', content: text }]);
  }

  statusIcon(state: 'running' | 'done' | 'error'): string {
    if (state === 'done') return 'task_alt';
    if (state === 'error') return 'error';
    return 'progress_activity';
  }

  private addStatusMessage(
    title: string,
    detail: string,
    state: 'running' | 'done' | 'error'
  ): number {
    let idx = -1;
    this.messages.update((all) => {
      idx = all.length;
      return [...all, { kind: 'status', title, detail, state }];
    });
    this.scrollMessagesToBottom();
    return idx;
  }

  private updateStatusMessage(
    idx: number,
    title: string,
    detail: string,
    state: 'running' | 'done' | 'error'
  ): void {
    this.messages.update((all) => {
      const copy = [...all];
      const target = copy[idx];
      if (target?.kind === 'status') {
        copy[idx] = { kind: 'status', title, detail, state };
      }
      return copy;
    });
    this.scrollMessagesToBottom();
  }

  private addPendingAssistant(): number {
    let idx = -1;
    this.messages.update((all) => { idx = all.length; return [...all, { kind: 'text', role: 'assistant', content: '' }]; });
    return idx;
  }

  private replaceAssistant(idx: number, content: string): void {
    this.messages.update((all) => {
      const copy = [...all];
      const t = copy[idx];
      if (t?.kind === 'text' && t.role === 'assistant') copy[idx] = { ...t, content };
      return copy;
    });
  }

  private formatAnalysis(items: FileAnalysisItem[]): string {
    if (!items.length) return 'Aucun fichier lisible dans la sélection.';
    const complete = items.filter((i) => !i.missingTags.length).length;
    const missing  = items.length - complete;
    let out = `ANALYSE — ${items.length} fichier(s)\n`;
    out += `─────────────────────────────────────\n`;
    out += `✓ ${complete} complet(s)   ✗ ${missing} avec tags manquants\n\n`;
    for (const item of items) {
      out += `${item.filename}\n`;
      const tags = Object.entries(item.currentTags).map(([k, v]) => `${k} : ${v}`).join('  |  ');
      if (tags) out += `  ${tags}\n`;
      if (item.missingTags.length) out += `  Manquants : ${item.missingTags.join(', ')}\n`;
      else out += `  ✓ Tags complets\n`;
    }
    return out.trim();
  }

  private formatEnrichment(items: FileEnrichItem[]): string {
    if (!items.length) return 'Aucun fichier traité.';
    const ok  = items.filter((i) => i.status === 'SUCCESS').length;
    const nf  = items.filter((i) => i.status === 'NOT_FOUND').length;
    const err = items.filter((i) => i.status === 'ERROR').length;
    let out = `ENRICHISSEMENT SPOTIFY — ${items.length} fichier(s)\n`;
    out += `─────────────────────────────────────\n`;
    out += `✓ ${ok} trouvé(s)   ✗ ${nf} introuvable(s)   ⚠ ${err} erreur(s)\n\n`;
    for (const item of items) {
      const icon = item.status === 'SUCCESS' ? '✓' : item.status === 'NOT_FOUND' ? '✗' : '⚠';
      out += `${icon} ${item.filename}\n`;
      if (item.status === 'SUCCESS' && item.metadata) {
        const m = item.metadata;
        const parts: string[] = [];
        if (m.album)                     parts.push(`Album : ${m.album}`);
        if (m.genres?.length)            parts.push(`Genre : ${m.genres.join(', ')}`);
        if (m.audioFeatures?.bpm)        parts.push(`BPM : ${Math.round(m.audioFeatures.bpm)}`);
        if (m.audioFeatures?.fullKey?.length) parts.push(`Tonalité : ${m.audioFeatures.fullKey}`);
        if (m.releaseYear)               parts.push(`Année : ${m.releaseYear}`);
        if (parts.length) out += `  ${parts.join('  |  ')}\n`;
      } else if (item.message) {
        out += `  ${item.message}\n`;
      }
    }
    return out.trim();
  }

  private scrollMessagesToBottom(): void {
    const el = this.messagesContainerRef?.nativeElement;
    if (el) el.scrollTop = el.scrollHeight;
  }

  private errorToMessage(error: unknown): string {
    const msg = error instanceof Error ? error.message : String(error);
    if (msg.includes('Failed to fetch') || msg.includes('ERR_CONNECTION_REFUSED') || msg.includes('NetworkError'))
      return "Impossible de joindre le serveur. Vérifie que le backend est démarré.";
    if (msg.includes('503') || msg.toLowerCase().includes('service unavailable'))
      return "Ollama est inaccessible. Lance : docker-compose up -d";
    if (msg.includes('504') || msg.includes('timeout'))
      return "Délai dépassé — Ollama met trop longtemps à répondre.";
    if (msg.includes('401') || msg.includes('403'))
      return "Accès refusé — vérifie les credentials Spotify/Soundcharts.";
    return msg || "Une erreur est survenue";
  }
}
