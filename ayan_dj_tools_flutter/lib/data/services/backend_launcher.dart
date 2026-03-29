import 'dart:async';
import 'dart:io';
import 'package:dio/dio.dart';
import 'package:path/path.dart' as p;
import '../../core/constants/api_endpoints.dart';

/// Manages the Spring Boot backend JAR lifecycle.
/// Mirrors the logic in electron/main.ts — find JAR, launch, poll health, kill on exit.
class BackendLauncher {
  Process? _process;
  bool _ready = false;

  bool get isReady => _ready;

  /// Locates the backend JAR relative to the executable (production layout):
  /// `<exe_dir>/data/flutter_assets/assets/backend/ayan-dj-tools.jar`
  /// Falls back to the dev layout (repo root):
  /// `<project_root>/infra/build/libs/ayan-dj-tools.jar`
  String? _findJar() {
    final exeDir = p.dirname(Platform.resolvedExecutable);

    // Production bundle
    final prodJar = p.join(exeDir, 'data', 'flutter_assets', 'assets', 'backend', 'ayan-dj-tools.jar');
    if (File(prodJar).existsSync()) return prodJar;

    // Dev layout: go up from exe dir until we find infra/build/libs/
    var dir = exeDir;
    for (var i = 0; i < 6; i++) {
      final candidate = p.join(dir, 'infra', 'build', 'libs', 'ayan-dj-tools.jar');
      if (File(candidate).existsSync()) return candidate;
      dir = p.dirname(dir);
    }
    return null;
  }

  /// Starts the backend JAR if found, then waits for /actuator/health to return UP.
  /// Returns true if backend is ready within [timeoutSeconds].
  Future<bool> launch({int timeoutSeconds = 30}) async {
    final jar = _findJar();
    if (jar != null) {
      _process = await Process.start(
        'java',
        ['-jar', jar],
        mode: ProcessStartMode.normal,
      );
      _process!.stdout.listen((_) {}); // drain stdout
      _process!.stderr.listen((_) {}); // drain stderr
      _process!.exitCode.then((_) => _ready = false);
    }
    // Poll health endpoint (whether we launched the JAR or assume it's running externally)
    _ready = await _waitForHealth(timeoutSeconds: timeoutSeconds);
    return _ready;
  }

  Future<bool> _waitForHealth({required int timeoutSeconds}) async {
    final dio = Dio(BaseOptions(
      baseUrl: ApiEndpoints.defaultBaseUrl,
      connectTimeout: const Duration(seconds: 2),
      receiveTimeout: const Duration(seconds: 2),
    ));
    final deadline = DateTime.now().add(Duration(seconds: timeoutSeconds));
    while (DateTime.now().isBefore(deadline)) {
      try {
        final res = await dio.get(ApiEndpoints.health);
        if ((res.data as Map<String, dynamic>?)?['status'] == 'UP') return true;
      } catch (_) {}
      await Future.delayed(const Duration(seconds: 1));
    }
    return false;
  }

  /// Kills the backend process gracefully. On Windows, uses taskkill /f /t.
  void shutdown() {
    final proc = _process;
    if (proc == null) return;
    if (Platform.isWindows) {
      Process.runSync('taskkill', ['/pid', proc.pid.toString(), '/f', '/t']);
    } else {
      proc.kill(ProcessSignal.sigterm);
    }
    _process = null;
    _ready = false;
  }
}

/// Singleton instance used app-wide.
final backendLauncher = BackendLauncher();
