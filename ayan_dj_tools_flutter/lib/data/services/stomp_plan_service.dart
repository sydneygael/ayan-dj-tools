import 'dart:async';
import 'dart:convert';

import 'package:stomp_dart_client/stomp_dart_client.dart';

import '../../core/constants/stomp_destinations.dart';
import '../models/models.dart';

/// STOMP WebSocket client for plan progress events.
/// Mirrors usePlanProgress.ts — subscribes to `/topic/plan/{planId}/progress`.
class StompPlanService {
  final String wsUrl;

  StompClient? _client;
  final _eventController = StreamController<TagProgressEvent>.broadcast();

  StompPlanService(this.wsUrl);

  Stream<TagProgressEvent> get eventStream => _eventController.stream;

  void connect(String planId) {
    _client?.deactivate();

    _client = StompClient(
      config: StompConfig(
        url: wsUrl,
        onConnect: (frame) {
          _client!.subscribe(
            destination: StompDestinations.planProgress(planId),
            callback: (f) {
              if (f.body == null) return;
              try {
                final event = TagProgressEvent.fromJson(
                  jsonDecode(f.body!) as Map<String, dynamic>,
                );
                _eventController.add(event);
              } catch (_) {}
            },
          );
        },
        onDisconnect: (_) {},
        onStompError: (_) {},
        onWebSocketError: (_) {},
        reconnectDelay: const Duration(seconds: 5),
      ),
    );
    _client!.activate();
  }

  void disconnect() {
    _client?.deactivate();
    _client = null;
  }

  void dispose() {
    disconnect();
    _eventController.close();
  }
}
