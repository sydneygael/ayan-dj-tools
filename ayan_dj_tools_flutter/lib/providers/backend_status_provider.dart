import 'dart:async';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../core/constants/api_endpoints.dart';
import 'settings_provider.dart';

/// Polls /actuator/health every 5 seconds and emits the backend readiness state.
final backendStatusProvider = StreamProvider<bool>((ref) {
  final baseUrl = ref.watch(settingsProvider).apiUrl;
  final dio = Dio(BaseOptions(
    baseUrl: baseUrl,
    connectTimeout: const Duration(seconds: 2),
    receiveTimeout: const Duration(seconds: 2),
  ));

  final controller = StreamController<bool>();

  Future<void> check() async {
    try {
      final res = await dio.get(ApiEndpoints.health);
      final status = (res.data as Map<String, dynamic>?)?['status'] ?? '';
      controller.add(status == 'UP');
    } catch (_) {
      controller.add(false);
    }
  }

  check();
  final timer = Timer.periodic(const Duration(seconds: 5), (_) => check());

  ref.onDispose(() {
    timer.cancel();
    controller.close();
  });

  return controller.stream;
});
