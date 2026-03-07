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

export interface MissingTagsReport {
  filepath: string;
  missingTags: string[];
}

export enum PlanStatus {
  DRAFT = 'DRAFT',
  APPROVED = 'APPROVED',
  EXECUTING = 'EXECUTING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
}

export enum OperationStatus {
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
  APPLIED = 'APPLIED',
  FAILED = 'FAILED',
}

export interface TagOperation {
  filepath: string;
  currentTags: Record<string, string>;
  suggestedTags: Record<string, string>;
  status: OperationStatus;
  message: string | null;
}

export interface TaggingPlan {
  planId: string;
  operations: TagOperation[];
  createdAt: string;
  status: PlanStatus;
  totalFiles: number;
  filesWithMissingTags: number;
}

export interface TagChange {
  tagName: string;
  oldValue: string | null;
  newValue: string;
}

export interface TagPreview {
  filepath: string;
  changes: TagChange[];
}

export interface TagWriteResult {
  filepath: string;
  success: boolean;
  message: string;
}

export interface BatchApplyResult {
  totalFiles: number;
  successCount: number;
  failureCount: number;
  results: TagWriteResult[];
}

export interface ChatMessage {
  role: 'user' | 'agent';
  content: string;
  timestamp: string;
}

export interface ChatRequest {
  message: string;
  conversationId?: string;
}

export interface ChatResponse {
  reply: string;
  conversationId: string;
  messageCount: number;
  timestamp: string;
}

export interface SimilarTrackResult {
  track: MusicFileInfo;
  similarityScore: number;
}

export interface SmartTagSuggestion {
  filepath: string;
  suggestedTags: Record<string, string>;
  similarTracks: SimilarTrackResult[];
  confidence: number;
  source: string;
}

export interface TaggingHistoryEntry {
  planId: string;
  filepath: string;
  changes: TagChange[];
  appliedAt: string;
  success: boolean;
}
