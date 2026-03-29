import 'dart:async';
import 'dart:convert';

import 'package:stomp_dart_client/stomp_dart_client.dart';

import '../../core/constants/stomp_destinations.dart';
import '../models/models.dart';

/// STOMP WebSocket client for chat streaming.
/// Mirrors useWebSocket.ts — connects to /ws, subscribes to
/// `/topic/responses/{conversationId}`, publishes to /app/chat.
class StompChatService {
  final String wsUrl;

  StompClient? _client;
  StompUnsubscribe? _subscription;

  final _eventController = StreamController<ChatStreamEvent>.broadcast();
  final _connectedController = StreamController<bool>.broadcast();

  bool _connected = false;

  StompChatService(this.wsUrl);

  Stream<ChatStreamEvent> get eventStream => _eventController.stream;
  Stream<bool> get connectedStream => _connectedController.stream;
  bool get isConnected => _connected;

  void connect(String conversationId) {
    if (_client?.connected ?? false) {
      _resubscribe(conversationId);
      return;
    }

    _client = StompClient(
      config: StompConfig(
        url: wsUrl,
        onConnect: (frame) {
          _connected = true;
          _connectedController.add(true);
          _resubscribe(conversationId);
        },
        onDisconnect: (_) {
          _connected = false;
          _connectedController.add(false);
        },
        onStompError: (_) {
          _connected = false;
          _connectedController.add(false);
        },
        onWebSocketError: (_) {
          _connected = false;
          _connectedController.add(false);
        },
        reconnectDelay: const Duration(seconds: 5),
      ),
    );
    _client!.activate();
  }

  void resubscribe(String conversationId) {
    if (_client?.connected ?? false) {
      _resubscribe(conversationId);
    }
  }

  void _resubscribe(String conversationId) {
    _subscription?.call();
    _subscription = _client!.subscribe(
      destination: StompDestinations.chatResponses(conversationId),
      callback: (frame) {
        if (frame.body == null) return;
        try {
          final event = ChatStreamEvent.fromJson(
            jsonDecode(frame.body!) as Map<String, dynamic>,
          );
          _eventController.add(event);
        } catch (_) {}
      },
    );
  }

  void sendMessage(String message, String conversationId) {
    if (!(_client?.connected ?? false)) return;
    _client!.send(
      destination: StompDestinations.chat,
      body: jsonEncode({'message': message, 'conversationId': conversationId}),
    );
  }

  void stopStream(String conversationId) {
    if (!(_client?.connected ?? false)) return;
    _client!.send(
      destination: StompDestinations.chatStop,
      body: jsonEncode({'conversationId': conversationId}),
    );
  }

  void disconnect() {
    _subscription?.call();
    _subscription = null;
    _client?.deactivate();
    _client = null;
    _connected = false;
    _connectedController.add(false);
  }

  void dispose() {
    disconnect();
    _eventController.close();
    _connectedController.close();
  }
}
