export type OperatingMode = 'PLAN' | 'MANUAL' | 'APPLY';
export type PlanStatus = 'DRAFT' | 'READY_FOR_REVIEW' | 'APPROVED' | 'APPLYING' | 'COMPLETED';
export type OperationStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'APPLIED' | 'ERROR';

export interface ChatMessage {
  role: string;
  content: string;
  timestamp?: string;
}

export interface ChatRequest {
  message: string;
  conversationId?: string | null;
  mode?: OperatingMode | null;
  filePaths?: string[] | null;
  currentDir?: string | null;
}

export interface ToolCallInfo {
  id: string;
  name: string;
  argumentsJson: string;
}

export interface ChatResponse {
  reply: string;
  conversationId: string;
  messageCount: number;
  timestamp: string;
  toolCalls?: ToolCallInfo[];
}

export interface ChatStreamEvent {
  type: 'chunk' | 'tool-call' | 'done' | 'error' | 'interrupted';
  token?: string | null;
  reply?: string | null;
  conversationId: string;
  messageCount?: number | null;
  timestamp?: string | null;
  toolCallId?: string | null;
  toolName?: string | null;
  toolArgsJson?: string | null;
  toolResultJson?: string | null;
}

export type UiMessage =
  | { kind: 'text'; role: 'user' | 'assistant'; content: string }
  | {
      kind: 'tool';
      toolCallId: string;
      name: string;
      argsJson: string;
      status: 'running' | 'done' | 'error';
      resultJson?: string;
    };

export interface TagOperation {
  filepath: string;
  currentTags: Record<string, string>;
  suggestedTags: Record<string, string>;
  status: OperationStatus;
  message?: string | null;
}

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

export interface TagWriteResult {
  filepath: string;
  status: OperationStatus;
  message?: string | null;
}

export interface BatchApplyResult {
  planId: string;
  totalOperations: number;
  successCount: number;
  errorCount: number;
  results: TagWriteResult[];
  duration?: string;
}

export interface TagChange {
  field: string;
  oldValue?: string | null;
  newValue?: string | null;
}

export interface TagPreview {
  filepath: string;
  changes: TagChange[];
}

export interface TaggingHistoryEntry {
  filepath: string;
  planId: string;
  oldTags: Record<string, string>;
  newTags: Record<string, string>;
  status: OperationStatus;
  errorMessage?: string | null;
  appliedAt: string;
}

export interface TagProgressEvent {
  planId: string;
  index: number;
  total: number;
  filepath: string;
  status: OperationStatus;
  message?: string | null;
}

export interface PlanProgressResponse {
  planId: string;
  status: PlanStatus;
  mode: OperatingMode;
  currentIndex: number;
  totalOperations: number;
  pendingCount: number;
  approvedCount: number;
  appliedCount: number;
  rejectedCount: number;
  errorCount: number;
}

export interface AudioFeatures {
  bpm?: number | null;
  fullKey?: string | null;
  danceability?: number | null;
  energy?: number | null;
  valence?: number | null;
}

export interface EnrichedTrackMetadata {
  artist?: string | null;
  title?: string | null;
  album?: string | null;
  genres?: string[];
  durationMs?: number | null;
  audioFeatures?: AudioFeatures | null;
}

export interface Playlist {
  playlistId: string;
  name: string;
  technique: string;
  tracks: EnrichedTrackMetadata[];
  createdAt: string;
}

export interface SimilarTrackResult {
  track: EnrichedTrackMetadata;
  similarityScore: number;
}

export interface StatsReport {
  totalPlansCreated: number;
  totalTagsApplied: number;
  totalFilesEnriched: number;
  tagsAppliedByType: Record<string, number>;
  recentActivity: TaggingHistoryEntry[];
}

export interface CollectionProfile {
  genreDistribution: Record<string, number>;
  bpmHistogram: Record<string, number>;
  keyDistribution: Record<string, number>;
  averageAudioFeatures: Record<string, number>;
  totalTracksScanned: number;
  totalTracksEnriched: number;
  totalWithCompleteTags: number;
}

export interface EnrichmentStats {
  spotifyMatchRate: number;
  mostEnrichedTagTypes: Record<string, number>;
  errorRate: number;
  enrichmentBySource: Record<string, number>;
}

export interface ActivityTimeline {
  plansPerPeriod: Record<string, number>;
  tagsAppliedPerPeriod: Record<string, number>;
  modeUsage: Record<string, number>;
  averageDurationByMode: Record<string, number>;
}

export interface BackendHealth {
  status?: string;
}

export interface FileEntry {
  name: string;
  absolutePath: string;
  isDirectory: boolean;
  fileSizeBytes: number;
  artist?: string | null;
  title?: string | null;
  album?: string | null;
  genre?: string | null;
  hasCompleteTags: boolean;
}

export interface FileBrowserPage {
  directory: string;
  page: number;
  pageSize: number;
  totalEntries: number;
  totalPages: number;
  entries: FileEntry[];
}
