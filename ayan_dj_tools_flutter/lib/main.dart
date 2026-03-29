import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'app.dart';
import 'data/services/backend_launcher.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await EasyLocalization.ensureInitialized();

  // In debug builds, assume the backend is already running (./gradlew infra:bootRun).
  // In release builds, launch the bundled JAR and show a splash until ready.
  final showSplash = !_isDebugBuild();

  runApp(
    EasyLocalization(
      supportedLocales: const [Locale('fr'), Locale('en')],
      path: 'assets/i18n',
      fallbackLocale: const Locale('fr'),
      startLocale: const Locale('fr'),
      saveLocale: true,
      child: ProviderScope(
        child: showSplash
            ? const _BackendBootstrap(child: AyanDjToolsApp())
            : const AyanDjToolsApp(),
      ),
    ),
  );
}

bool _isDebugBuild() {
  bool debug = false;
  assert(() {
    debug = true;
    return true;
  }());
  return debug;
}

/// Splash widget: launches the backend JAR then shows the real app.
class _BackendBootstrap extends StatefulWidget {
  final Widget child;

  const _BackendBootstrap({required this.child});

  @override
  State<_BackendBootstrap> createState() => _BackendBootstrapState();
}

class _BackendBootstrapState extends State<_BackendBootstrap> {
  bool _ready = false;
  bool _failed = false;

  @override
  void initState() {
    super.initState();
    _start();
  }

  Future<void> _start() async {
    setState(() {
      _ready = false;
      _failed = false;
    });
    final ok = await backendLauncher.launch();
    if (mounted) {
      setState(() {
        _ready = true;
        _failed = !ok;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_ready && !_failed) return widget.child;

    return MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: ThemeData.dark(useMaterial3: true),
      home: Scaffold(
        body: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              if (!_ready) ...[
                const CircularProgressIndicator(),
                const SizedBox(height: 24),
                const Text('Demarrage du backend...', style: TextStyle(fontSize: 16)),
              ] else ...[
                const Icon(Icons.error_outline, size: 48, color: Colors.red),
                const SizedBox(height: 16),
                const Text('Backend non disponible', style: TextStyle(fontSize: 16)),
                const SizedBox(height: 16),
                ElevatedButton(
                  onPressed: _start,
                  child: const Text('Reessayer'),
                ),
                TextButton(
                  onPressed: () => setState(() {
                    _ready = true;
                    _failed = false;
                  }),
                  child: const Text('Continuer sans backend'),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  @override
  void dispose() {
    backendLauncher.shutdown();
    super.dispose();
  }
}
