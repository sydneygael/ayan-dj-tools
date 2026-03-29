class ApiEndpoints {
  ApiEndpoints._();

  static const String defaultBaseUrl = 'http://localhost:8080';
  static const String defaultWsUrl = 'http://localhost:8080/ws';

  static const String health = '/actuator/health';

  // Agent
  static const String agentChat = '/api/agent/chat';
  static String conversationHistory(String id) => '/api/agent/conversations/$id/history';
  static String deleteConversation(String id) => '/api/agent/conversations/$id';

  // Plan
  static const String planCreate = '/api/plan/create';
  static String plan(String id) => '/api/plan/$id';
  static String planApprove(String id) => '/api/plan/$id/approve';
  static String planExecute(String id) => '/api/plan/$id/execute';
  static String planPreview(String id) => '/api/plan/$id/preview';
  static String planHistory(String id) => '/api/plan/$id/history';
  static String planCurrent(String id) => '/api/plan/$id/current';
  static String planConfirmOperation(String id, int index) =>
      '/api/plan/$id/operations/$index/confirm';
  static String planAutoExecute(String id) => '/api/plan/$id/auto-execute';

  // Tags
  static const String tagsApply = '/api/tags/apply';
  static const String tagsPreview = '/api/tags/preview';

  // Library
  static const String library = '/api/library';

  // Stats
  static const String stats = '/api/stats';
  static const String statsCollection = '/api/stats/collection';
  static const String statsEnrichment = '/api/stats/enrichment';
  static const String statsActivity = '/api/stats/activity';

  // Playlist
  static const String playlistGenerate = '/api/playlist/generate';

  // RAG
  static const String ragSimilar = '/api/rag/similar';
}
