import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/models/models.dart';
import '../data/repositories/agent_repository.dart';
import '../data/services/stomp_chat_service.dart';
import 'settings_provider.dart';

class ChatState {
  final List<ChatMessage> messages;
  final String conversationId;
  final bool loading;
  final String? streamingContent;
  final bool wsConnected;

  const ChatState({
    this.messages = const [],
    this.conversationId = '',
    this.loading = false,
    this.streamingContent,
    this.wsConnected = false,
  });

  ChatState copyWith({
    List<ChatMessage>? messages,
    String? conversationId,
    bool? loading,
    String? Function()? streamingContent,
    bool? wsConnected,
  }) =>
      ChatState(
        messages: messages ?? this.messages,
        conversationId: conversationId ?? this.conversationId,
        loading: loading ?? this.loading,
        streamingContent:
            streamingContent != null ? streamingContent() : this.streamingContent,
        wsConnected: wsConnected ?? this.wsConnected,
      );
}

class ChatNotifier extends Notifier<ChatState> {
  late StompChatService _stomp;
  StreamSubscription<ChatStreamEvent>? _eventSub;
  StreamSubscription<bool>? _connSub;

  @override
  ChatState build() {
    final settings = ref.watch(settingsProvider);
    final wsUrl = settings.wsUrl;
    _stomp = StompChatService(wsUrl);
    ref.onDispose(() {
      _eventSub?.cancel();
      _connSub?.cancel();
      _stomp.dispose();
    });
    return const ChatState();
  }

  void connectWs() {
    _connSub?.cancel();
    _connSub = _stomp.connectedStream.listen((connected) {
      state = state.copyWith(wsConnected: connected);
    });

    _eventSub?.cancel();
    _eventSub = _stomp.eventStream.listen(_handleEvent);

    _stomp.connect(state.conversationId);
  }

  void disconnectWs() {
    _stomp.disconnect();
    state = state.copyWith(wsConnected: false);
  }

  void _handleEvent(ChatStreamEvent event) {
    switch (event.type) {
      case 'chunk':
        final current = state.streamingContent ?? '';
        state = state.copyWith(
          streamingContent: () => current + (event.token ?? ''),
          loading: false,
        );
      case 'done':
        _finalizeStream(
          event.reply ?? state.streamingContent ?? '',
          event.timestamp ?? DateTime.now().toIso8601String(),
          event.conversationId,
        );
      case 'interrupted':
        _finalizeStream(
          event.reply ?? state.streamingContent ?? '',
          DateTime.now().toIso8601String(),
          event.conversationId,
        );
      case 'error':
        final messages = List<ChatMessage>.from(state.messages)
          ..add(ChatMessage(
            role: 'agent',
            content: 'Erreur : ${event.token ?? "inconnue"}',
            timestamp: DateTime.now().toIso8601String(),
          ));
        state = state.copyWith(
          messages: messages,
          loading: false,
          streamingContent: () => null,
        );
    }
  }

  void _finalizeStream(String content, String timestamp, String convId) {
    final messages = List<ChatMessage>.from(state.messages)
      ..add(ChatMessage(role: 'agent', content: content, timestamp: timestamp));
    state = state.copyWith(
      messages: messages,
      loading: false,
      streamingContent: () => null,
      conversationId: convId.isNotEmpty ? convId : null,
    );
  }

  Future<void> sendMessage(String text) async {
    if (text.isEmpty || state.loading) return;

    final now = DateTime.now().toIso8601String();
    final messages = List<ChatMessage>.from(state.messages)
      ..add(ChatMessage(role: 'user', content: text, timestamp: now));

    state = state.copyWith(messages: messages, loading: true);

    if (state.wsConnected && ref.read(settingsProvider).wsEnabled) {
      _stomp.resubscribe(state.conversationId);
      _stomp.sendMessage(text, state.conversationId);
    } else {
      // REST fallback
      try {
        final repo = ref.read(agentRepositoryProvider);
        final res = await repo.chat(text, state.conversationId);
        final updatedMessages = List<ChatMessage>.from(state.messages)
          ..add(ChatMessage(
            role: 'agent',
            content: res.reply,
            timestamp: res.timestamp, // ChatRestResponse
          ));
        state = state.copyWith(
          messages: updatedMessages,
          conversationId: res.conversationId,
          loading: false,
        );
      } catch (_) {
        final updatedMessages = List<ChatMessage>.from(state.messages)
          ..add(ChatMessage(
            role: 'agent',
            content: 'chat.connectionError',
            timestamp: DateTime.now().toIso8601String(),
          ));
        state = state.copyWith(
          messages: updatedMessages,
          loading: false,
        );
      }
    }
  }

  void stopStream() {
    _stomp.stopStream(state.conversationId);
  }
}

final chatProvider = NotifierProvider<ChatNotifier, ChatState>(
  ChatNotifier.new,
);
