// All domain models mirroring the TypeScript types.ts and Java records.

// ---------------------------------------------------------------------------
// Enums
// ---------------------------------------------------------------------------

enum OperatingMode { plan, manual, apply }

enum PlanStatus { draft, readyForReview, approved, applying, completed }

enum OperationStatus { pending, approved, rejected, applied, error }

extension PlanStatusX on PlanStatus {
  static PlanStatus fromString(String s) => switch (s.toUpperCase()) {
        'DRAFT' => PlanStatus.draft,
        'READY_FOR_REVIEW' => PlanStatus.readyForReview,
        'APPROVED' => PlanStatus.approved,
        'APPLYING' => PlanStatus.applying,
        'COMPLETED' => PlanStatus.completed,
        _ => PlanStatus.draft,
      };
}

extension OperationStatusX on OperationStatus {
  static OperationStatus fromString(String s) => switch (s.toUpperCase()) {
        'PENDING' => OperationStatus.pending,
        'APPROVED' => OperationStatus.approved,
        'REJECTED' => OperationStatus.rejected,
        'APPLIED' => OperationStatus.applied,
        'ERROR' => OperationStatus.error,
        _ => OperationStatus.pending,
      };
}

extension OperatingModeX on OperatingMode {
  static OperatingMode fromString(String s) => switch (s.toUpperCase()) {
        'PLAN' => OperatingMode.plan,
        'MANUAL' => OperatingMode.manual,
        'APPLY' => OperatingMode.apply,
        _ => OperatingMode.plan,
      };

  String toApiString() => name.toUpperCase();
}

// ---------------------------------------------------------------------------
// Music file
// ---------------------------------------------------------------------------

class MusicFileInfo {
  final String filepath;
  final String filename;
  final String? artist;
  final String? title;
  final String? album;
  final String? genre;
  final double? bpm;
  final String? key;
  final int fileSize;
  final String lastModified;

  const MusicFileInfo({
    required this.filepath,
    required this.filename,
    this.artist,
    this.title,
    this.album,
    this.genre,
    this.bpm,
    this.key,
    required this.fileSize,
    required this.lastModified,
  });

  factory MusicFileInfo.fromJson(Map<String, dynamic> json) => MusicFileInfo(
        filepath: json['filepath'] as String,
        filename: json['filename'] as String,
        artist: json['artist'] as String?,
        title: json['title'] as String?,
        album: json['album'] as String?,
        genre: json['genre'] as String?,
        bpm: (json['bpm'] as num?)?.toDouble(),
        key: json['key'] as String?,
        fileSize: (json['fileSize'] as num).toInt(),
        lastModified: json['lastModified'] as String,
      );
}

// ---------------------------------------------------------------------------
// Tag change & preview
// ---------------------------------------------------------------------------

class TagChange {
  final String tagName;
  final String? oldValue;
  final String newValue;

  const TagChange({required this.tagName, this.oldValue, required this.newValue});

  factory TagChange.fromJson(Map<String, dynamic> json) => TagChange(
        tagName: json['tagName'] as String,
        oldValue: json['oldValue'] as String?,
        newValue: json['newValue'] as String,
      );
}

class TagPreview {
  final String filepath;
  final List<TagChange> changes;

  const TagPreview({required this.filepath, required this.changes});

  factory TagPreview.fromJson(Map<String, dynamic> json) => TagPreview(
        filepath: json['filepath'] as String,
        changes: (json['changes'] as List)
            .map((e) => TagChange.fromJson(e as Map<String, dynamic>))
            .toList(),
      );
}

// ---------------------------------------------------------------------------
// Tag operation & plan
// ---------------------------------------------------------------------------

class TagOperation {
  final String filepath;
  final Map<String, String> currentTags;
  final Map<String, String> suggestedTags;
  final OperationStatus status;
  final String? message;

  const TagOperation({
    required this.filepath,
    required this.currentTags,
    required this.suggestedTags,
    required this.status,
    this.message,
  });

  TagOperation copyWith({OperationStatus? status, String? message}) => TagOperation(
        filepath: filepath,
        currentTags: currentTags,
        suggestedTags: suggestedTags,
        status: status ?? this.status,
        message: message ?? this.message,
      );

  factory TagOperation.fromJson(Map<String, dynamic> json) => TagOperation(
        filepath: json['filepath'] as String,
        currentTags: Map<String, String>.from(json['currentTags'] as Map),
        suggestedTags: Map<String, String>.from(json['suggestedTags'] as Map),
        status: OperationStatusX.fromString(json['status'] as String),
        message: json['message'] as String?,
      );
}

class TaggingPlan {
  final String planId;
  final List<TagOperation> operations;
  final String createdAt;
  final PlanStatus status;
  final int totalFiles;
  final int filesWithMissingTags;
  final OperatingMode mode;
  final int currentIndex;

  const TaggingPlan({
    required this.planId,
    required this.operations,
    required this.createdAt,
    required this.status,
    required this.totalFiles,
    required this.filesWithMissingTags,
    required this.mode,
    required this.currentIndex,
  });

  factory TaggingPlan.fromJson(Map<String, dynamic> json) => TaggingPlan(
        planId: json['planId'] as String,
        operations: (json['operations'] as List)
            .map((e) => TagOperation.fromJson(e as Map<String, dynamic>))
            .toList(),
        createdAt: json['createdAt'] as String,
        status: PlanStatusX.fromString(json['status'] as String),
        totalFiles: (json['totalFiles'] as num).toInt(),
        filesWithMissingTags: (json['filesWithMissingTags'] as num).toInt(),
        mode: OperatingModeX.fromString(json['mode'] as String),
        currentIndex: (json['currentIndex'] as num?)?.toInt() ?? 0,
      );
}

// ---------------------------------------------------------------------------
// Batch apply result
// ---------------------------------------------------------------------------

class TagWriteResult {
  final String filepath;
  final OperationStatus status;
  final String message;

  const TagWriteResult({
    required this.filepath,
    required this.status,
    required this.message,
  });

  factory TagWriteResult.fromJson(Map<String, dynamic> json) => TagWriteResult(
        filepath: json['filepath'] as String,
        status: OperationStatusX.fromString(json['status'] as String),
        message: json['message'] as String? ?? '',
      );
}

class BatchApplyResult {
  final String planId;
  final int totalOperations;
  final int successCount;
  final int errorCount;
  final List<TagWriteResult> results;
  final String duration;

  const BatchApplyResult({
    required this.planId,
    required this.totalOperations,
    required this.successCount,
    required this.errorCount,
    required this.results,
    required this.duration,
  });

  factory BatchApplyResult.fromJson(Map<String, dynamic> json) => BatchApplyResult(
        planId: json['planId'] as String,
        totalOperations: (json['totalOperations'] as num).toInt(),
        successCount: (json['successCount'] as num).toInt(),
        errorCount: (json['errorCount'] as num).toInt(),
        results: (json['results'] as List? ?? [])
            .map((e) => TagWriteResult.fromJson(e as Map<String, dynamic>))
            .toList(),
        duration: json['duration'] as String? ?? '',
      );
}

// ---------------------------------------------------------------------------
// WebSocket events
// ---------------------------------------------------------------------------

class TagProgressEvent {
  final String planId;
  final int index;
  final int total;
  final String filepath;
  final OperationStatus status;
  final String message;

  const TagProgressEvent({
    required this.planId,
    required this.index,
    required this.total,
    required this.filepath,
    required this.status,
    required this.message,
  });

  factory TagProgressEvent.fromJson(Map<String, dynamic> json) => TagProgressEvent(
        planId: json['planId'] as String,
        index: (json['index'] as num).toInt(),
        total: (json['total'] as num).toInt(),
        filepath: json['filepath'] as String,
        status: OperationStatusX.fromString(json['status'] as String),
        message: json['message'] as String? ?? '',
      );
}

// ---------------------------------------------------------------------------
// Chat
// ---------------------------------------------------------------------------

class ChatMessage {
  final String role; // 'user' | 'agent'
  final String content;
  final String timestamp;

  const ChatMessage({
    required this.role,
    required this.content,
    required this.timestamp,
  });

  factory ChatMessage.fromJson(Map<String, dynamic> json) => ChatMessage(
        role: json['role'] as String,
        content: json['content'] as String,
        timestamp: json['timestamp'] as String,
      );

  Map<String, dynamic> toJson() => {
        'role': role,
        'content': content,
        'timestamp': timestamp,
      };
}

class ChatStreamEvent {
  final String type; // 'chunk' | 'done' | 'error' | 'interrupted'
  final String? token;
  final String? reply;
  final String conversationId;
  final int? messageCount;
  final String? timestamp;

  const ChatStreamEvent({
    required this.type,
    this.token,
    this.reply,
    required this.conversationId,
    this.messageCount,
    this.timestamp,
  });

  factory ChatStreamEvent.fromJson(Map<String, dynamic> json) => ChatStreamEvent(
        type: json['type'] as String,
        token: json['token'] as String?,
        reply: json['reply'] as String?,
        conversationId: json['conversationId'] as String? ?? '',
        messageCount: (json['messageCount'] as num?)?.toInt(),
        timestamp: json['timestamp'] as String?,
      );
}

// ---------------------------------------------------------------------------
// History
// ---------------------------------------------------------------------------

class TaggingHistoryEntry {
  final String planId;
  final String filepath;
  final List<TagChange> changes;
  final String appliedAt;
  final bool success;

  const TaggingHistoryEntry({
    required this.planId,
    required this.filepath,
    required this.changes,
    required this.appliedAt,
    required this.success,
  });

  factory TaggingHistoryEntry.fromJson(Map<String, dynamic> json) => TaggingHistoryEntry(
        planId: json['planId'] as String,
        filepath: json['filepath'] as String,
        changes: (json['changes'] as List? ?? [])
            .map((e) => TagChange.fromJson(e as Map<String, dynamic>))
            .toList(),
        appliedAt: json['appliedAt'] as String,
        success: json['success'] as bool? ?? false,
      );
}

// ---------------------------------------------------------------------------
// Stats
// ---------------------------------------------------------------------------

class StatsReport {
  final int totalPlansCreated;
  final int totalTagsApplied;
  final int totalFilesEnriched;
  final Map<String, int> tagsAppliedByType;
  final List<TaggingHistoryEntry> recentActivity;

  const StatsReport({
    required this.totalPlansCreated,
    required this.totalTagsApplied,
    required this.totalFilesEnriched,
    required this.tagsAppliedByType,
    required this.recentActivity,
  });

  factory StatsReport.fromJson(Map<String, dynamic> json) => StatsReport(
        totalPlansCreated: (json['totalPlansCreated'] as num).toInt(),
        totalTagsApplied: (json['totalTagsApplied'] as num).toInt(),
        totalFilesEnriched: (json['totalFilesEnriched'] as num).toInt(),
        tagsAppliedByType: Map<String, int>.from(
          (json['tagsAppliedByType'] as Map? ?? {}).map(
            (k, v) => MapEntry(k as String, (v as num).toInt()),
          ),
        ),
        recentActivity: (json['recentActivity'] as List? ?? [])
            .map((e) => TaggingHistoryEntry.fromJson(e as Map<String, dynamic>))
            .toList(),
      );
}

class CollectionProfile {
  final Map<String, int> genreDistribution;
  final Map<String, int> bpmHistogram;
  final Map<String, int> keyDistribution;
  final Map<String, double> averageAudioFeatures;
  final int totalTracksScanned;
  final int totalTracksEnriched;
  final int totalWithCompleteTags;

  const CollectionProfile({
    required this.genreDistribution,
    required this.bpmHistogram,
    required this.keyDistribution,
    required this.averageAudioFeatures,
    required this.totalTracksScanned,
    required this.totalTracksEnriched,
    required this.totalWithCompleteTags,
  });

  factory CollectionProfile.fromJson(Map<String, dynamic> json) => CollectionProfile(
        genreDistribution: Map<String, int>.from(
          (json['genreDistribution'] as Map? ?? {}).map(
            (k, v) => MapEntry(k as String, (v as num).toInt()),
          ),
        ),
        bpmHistogram: Map<String, int>.from(
          (json['bpmHistogram'] as Map? ?? {}).map(
            (k, v) => MapEntry(k as String, (v as num).toInt()),
          ),
        ),
        keyDistribution: Map<String, int>.from(
          (json['keyDistribution'] as Map? ?? {}).map(
            (k, v) => MapEntry(k as String, (v as num).toInt()),
          ),
        ),
        averageAudioFeatures: Map<String, double>.from(
          (json['averageAudioFeatures'] as Map? ?? {}).map(
            (k, v) => MapEntry(k as String, (v as num).toDouble()),
          ),
        ),
        totalTracksScanned: (json['totalTracksScanned'] as num).toInt(),
        totalTracksEnriched: (json['totalTracksEnriched'] as num).toInt(),
        totalWithCompleteTags: (json['totalWithCompleteTags'] as num).toInt(),
      );
}

class EnrichmentStats {
  final double spotifyMatchRate;
  final Map<String, int> mostEnrichedTagTypes;
  final double errorRate;
  final Map<String, int> enrichmentBySource;

  const EnrichmentStats({
    required this.spotifyMatchRate,
    required this.mostEnrichedTagTypes,
    required this.errorRate,
    required this.enrichmentBySource,
  });

  factory EnrichmentStats.fromJson(Map<String, dynamic> json) => EnrichmentStats(
        spotifyMatchRate: (json['spotifyMatchRate'] as num).toDouble(),
        mostEnrichedTagTypes: Map<String, int>.from(
          (json['mostEnrichedTagTypes'] as Map? ?? {}).map(
            (k, v) => MapEntry(k as String, (v as num).toInt()),
          ),
        ),
        errorRate: (json['errorRate'] as num).toDouble(),
        enrichmentBySource: Map<String, int>.from(
          (json['enrichmentBySource'] as Map? ?? {}).map(
            (k, v) => MapEntry(k as String, (v as num).toInt()),
          ),
        ),
      );
}

class ActivityTimeline {
  final Map<String, int> plansPerPeriod;
  final Map<String, int> tagsAppliedPerPeriod;
  final Map<String, int> modeUsage;
  final Map<String, double> averageDurationByMode;

  const ActivityTimeline({
    required this.plansPerPeriod,
    required this.tagsAppliedPerPeriod,
    required this.modeUsage,
    required this.averageDurationByMode,
  });

  factory ActivityTimeline.fromJson(Map<String, dynamic> json) => ActivityTimeline(
        plansPerPeriod: Map<String, int>.from(
          (json['plansPerPeriod'] as Map? ?? {}).map(
            (k, v) => MapEntry(k as String, (v as num).toInt()),
          ),
        ),
        tagsAppliedPerPeriod: Map<String, int>.from(
          (json['tagsAppliedPerPeriod'] as Map? ?? {}).map(
            (k, v) => MapEntry(k as String, (v as num).toInt()),
          ),
        ),
        modeUsage: Map<String, int>.from(
          (json['modeUsage'] as Map? ?? {}).map(
            (k, v) => MapEntry(k as String, (v as num).toInt()),
          ),
        ),
        averageDurationByMode: Map<String, double>.from(
          (json['averageDurationByMode'] as Map? ?? {}).map(
            (k, v) => MapEntry(k as String, (v as num).toDouble()),
          ),
        ),
      );
}

// ---------------------------------------------------------------------------
// Playlist
// ---------------------------------------------------------------------------

class AudioFeatures {
  final double? bpm;
  final String? musicalKey;
  final String? mode;
  final double? danceability;
  final double? energy;
  final double? valence;

  const AudioFeatures({
    this.bpm,
    this.musicalKey,
    this.mode,
    this.danceability,
    this.energy,
    this.valence,
  });

  factory AudioFeatures.fromJson(Map<String, dynamic> json) => AudioFeatures(
        bpm: (json['bpm'] as num?)?.toDouble(),
        musicalKey: json['musicalKey'] as String?,
        mode: json['mode'] as String?,
        danceability: (json['danceability'] as num?)?.toDouble(),
        energy: (json['energy'] as num?)?.toDouble(),
        valence: (json['valence'] as num?)?.toDouble(),
      );
}

class EnrichedTrackMetadata {
  final String sourceId;
  final String artist;
  final String title;
  final String album;
  final List<String> genres;
  final int releaseYear;
  final int popularity;
  final int durationMs;
  final AudioFeatures? audioFeatures;

  const EnrichedTrackMetadata({
    required this.sourceId,
    required this.artist,
    required this.title,
    required this.album,
    required this.genres,
    required this.releaseYear,
    required this.popularity,
    required this.durationMs,
    this.audioFeatures,
  });

  factory EnrichedTrackMetadata.fromJson(Map<String, dynamic> json) => EnrichedTrackMetadata(
        sourceId: json['sourceId'] as String,
        artist: json['artist'] as String,
        title: json['title'] as String,
        album: json['album'] as String,
        genres: List<String>.from(json['genres'] as List? ?? []),
        releaseYear: (json['releaseYear'] as num).toInt(),
        popularity: (json['popularity'] as num).toInt(),
        durationMs: (json['durationMs'] as num).toInt(),
        audioFeatures: json['audioFeatures'] != null
            ? AudioFeatures.fromJson(json['audioFeatures'] as Map<String, dynamic>)
            : null,
      );
}

class Playlist {
  final String playlistId;
  final String name;
  final String technique;
  final List<EnrichedTrackMetadata> tracks;
  final String createdAt;

  const Playlist({
    required this.playlistId,
    required this.name,
    required this.technique,
    required this.tracks,
    required this.createdAt,
  });

  factory Playlist.fromJson(Map<String, dynamic> json) => Playlist(
        playlistId: json['playlistId'] as String,
        name: json['name'] as String,
        technique: json['technique'] as String,
        tracks: (json['tracks'] as List? ?? [])
            .map((e) => EnrichedTrackMetadata.fromJson(e as Map<String, dynamic>))
            .toList(),
        createdAt: json['createdAt'] as String,
      );
}

// ---------------------------------------------------------------------------
// Agent question
// ---------------------------------------------------------------------------

class AgentQuestion {
  final String questionId;
  final String filepath;
  final String type;
  final String question;
  final List<String> options;
  final String context;
  final double currentConfidence;

  const AgentQuestion({
    required this.questionId,
    required this.filepath,
    required this.type,
    required this.question,
    required this.options,
    required this.context,
    required this.currentConfidence,
  });

  factory AgentQuestion.fromJson(Map<String, dynamic> json) => AgentQuestion(
        questionId: json['questionId'] as String,
        filepath: json['filepath'] as String,
        type: json['type'] as String,
        question: json['question'] as String,
        options: List<String>.from(json['options'] as List? ?? []),
        context: json['context'] as String,
        currentConfidence: (json['currentConfidence'] as num).toDouble(),
      );
}
