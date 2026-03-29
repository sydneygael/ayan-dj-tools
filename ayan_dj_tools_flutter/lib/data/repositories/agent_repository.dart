import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/constants/api_endpoints.dart';
import '../../providers/settings_provider.dart';
import '../models/models.dart';

class AgentRepository {
  final Dio _dio;

  AgentRepository(String baseUrl)
      : _dio = Dio(BaseOptions(
          baseUrl: baseUrl,
          connectTimeout: const Duration(seconds: 30),
          receiveTimeout: const Duration(seconds: 60),
        ));

  /// REST fallback for chat when WebSocket is unavailable.
  Future<ChatRestResponse> chat(String message, String conversationId) async {
    final res = await _dio.post(
      ApiEndpoints.agentChat,
      data: {
        'message': message,
        if (conversationId.isNotEmpty) 'conversationId': conversationId,
      },
    );
    final data = res.data as Map<String, dynamic>;
    return ChatRestResponse(
      reply: data['reply'] as String,
      conversationId: data['conversationId'] as String,
      timestamp: data['timestamp'] as String? ?? DateTime.now().toIso8601String(),
    );
  }

  Future<List<ChatMessage>> getHistory(String conversationId) async {
    final res = await _dio.get(ApiEndpoints.conversationHistory(conversationId));
    return (res.data as List)
        .map((e) => ChatMessage.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<void> deleteConversation(String conversationId) async {
    await _dio.delete(ApiEndpoints.deleteConversation(conversationId));
  }
}

class ChatRestResponse {
  final String reply;
  final String conversationId;
  final String timestamp;

  const ChatRestResponse({
    required this.reply,
    required this.conversationId,
    required this.timestamp,
  });
}

final agentRepositoryProvider = Provider<AgentRepository>((ref) {
  final baseUrl = ref.watch(settingsProvider).apiUrl;
  return AgentRepository(baseUrl);
});
