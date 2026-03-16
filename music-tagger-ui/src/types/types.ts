/** Informations d'un fichier audio scanne. Correspond a {@code MusicFileInfo} cote Java. */
export interface MusicFileInfo {
  filepath: string;
  filename: string;
  artist: string | null;
  title: string | null;
  album: string | null;
  genre: string | null;
  bpm: number | null;
  key: string | null;
  fileSize: number;
  lastModified: string;
}

/** Rapport des tags manquants pour un fichier. Correspond a {@code MissingTagsReport} cote Java. */
export interface MissingTagsReport {
  filepath: string;
  missingTags: string[];
}

/** Cycle de vie d'un plan de tagging : DRAFT → READY_FOR_REVIEW → APPROVED → APPLYING → COMPLETED. */
export const PlanStatus = {
  DRAFT: 'DRAFT',
  READY_FOR_REVIEW: 'READY_FOR_REVIEW',
  APPROVED: 'APPROVED',
  APPLYING: 'APPLYING',
  COMPLETED: 'COMPLETED',
} as const;
export type PlanStatus = (typeof PlanStatus)[keyof typeof PlanStatus];

/** Statut d'une operation individuelle dans un plan : PENDING → APPROVED/REJECTED → APPLIED/ERROR. */
export const OperationStatus = {
  PENDING: 'PENDING',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
  APPLIED: 'APPLIED',
  ERROR: 'ERROR',
} as const;
export type OperationStatus = (typeof OperationStatus)[keyof typeof OperationStatus];

/** Operation de modification de tags pour un fichier. Correspond a {@code TagOperation} cote Java. */
export interface TagOperation {
  filepath: string;
  currentTags: Record<string, string>;
  suggestedTags: Record<string, string>;
  status: OperationStatus;
  message: string | null;
}

/** Plan de tagging regroupant les operations pour un ensemble de fichiers. Correspond a {@code TaggingPlan} cote Java. */
export interface TaggingPlan {
  planId: string;
  operations: TagOperation[];
  createdAt: string;
  status: PlanStatus;
  totalFiles: number;
  filesWithMissingTags: number;
  mode: OperatingMode;
  currentIndex: number;
}

/** Changement unitaire d'un tag (ancienne valeur → nouvelle valeur). */
export interface TagChange {
  tagName: string;
  oldValue: string | null;
  newValue: string;
}

/** Apercu des changements de tags avant application sur un fichier. */
export interface TagPreview {
  filepath: string;
  changes: TagChange[];
}

/** Resultat de l'ecriture de tags sur un fichier individuel. */
export interface TagWriteResult {
  filepath: string;
  status: OperationStatus;
  message: string;
}

/** Resultat agrege de l'application de tags sur un lot de fichiers. */
export interface BatchApplyResult {
  planId: string;
  totalOperations: number;
  successCount: number;
  errorCount: number;
  results: TagWriteResult[];
  duration: string;
}

/** Evenement de progression envoyé via WebSocket pendant l'execution d'un plan. */
export interface TagProgressEvent {
  planId: string;
  index: number;
  total: number;
  filepath: string;
  status: OperationStatus;
  message: string;
}

/** Message de conversation (utilisateur ou agent Ayan). */
export interface ChatMessage {
  role: 'user' | 'agent';
  content: string;
  timestamp: string;
}

/** Requete envoyee a l'agent via REST ou WebSocket. */
export interface ChatRequest {
  message: string;
  conversationId?: string;
}

/** Reponse de l'agent Ayan a un message de chat. */
export interface ChatResponse {
  reply: string;
  conversationId: string;
  messageCount: number;
  timestamp: string;
}

/** Resultat de recherche de pistes similaires via le RAG (Qdrant). */
export interface SimilarTrackResult {
  track: MusicFileInfo;
  similarityScore: number;
}

/** Suggestion intelligente de tags combinant Spotify et RAG. */
export interface SmartTagSuggestion {
  filepath: string;
  suggestedTags: Record<string, string>;
  similarTracks: SimilarTrackResult[];
  confidence: number;
  source: string;
}

/** Entree d'historique : trace d'une modification de tags appliquee. */
export interface TaggingHistoryEntry {
  planId: string;
  filepath: string;
  changes: TagChange[];
  appliedAt: string;
  success: boolean;
}

/** Mode de fonctionnement de l'agent : PLAN (lot revise), MANUAL (un par un), APPLY (auto). */
export type OperatingMode = 'PLAN' | 'MANUAL' | 'APPLY';

/** Question posee par l'agent a l'utilisateur pour lever une ambiguite. */
export interface AgentQuestion {
  questionId: string;
  filepath: string;
  type: string;
  question: string;
  options: string[];
  context: string;
  currentConfidence: number;
}

/** Reponse de l'utilisateur a une question de l'agent. */
export interface AgentQuestionResponse {
  questionId: string;
  selectedOption: string;
  applyToSimilar: boolean;
}

/** Donnees du ConfirmDialog. */
export interface ConfirmDialogData {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  warn?: boolean;
}

/** Statistiques agregees de la collection — retournees par GET /api/stats. */
export interface StatsReport {
  totalPlansCreated: number;
  totalTagsApplied: number;
  totalFilesEnriched: number;
  tagsAppliedByType: Record<string, number>;
  recentActivity: TaggingHistoryEntry[];
}
