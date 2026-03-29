class StompDestinations {
  StompDestinations._();

  // Publish destinations (client → server)
  static const String chat = '/app/chat';
  static const String chatStop = '/app/chat/stop';

  // Subscribe destinations (server → client)
  static String chatResponses(String conversationId) =>
      '/topic/responses/$conversationId';
  static String planProgress(String planId) =>
      '/topic/plan/$planId/progress';
}
