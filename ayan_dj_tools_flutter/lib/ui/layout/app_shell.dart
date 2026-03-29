import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'top_bar.dart';
import 'sidebar.dart';

/// Root layout: top bar + persistent sidebar (280px) + routed content area.
/// Keyboard shortcuts are registered here at the shell level.
class AppShell extends StatelessWidget {
  final Widget child;

  const AppShell({super.key, required this.child});

  @override
  Widget build(BuildContext context) {
    return Shortcuts(
      shortcuts: {
        // Mode shortcuts
        LogicalKeySet(LogicalKeyboardKey.control, LogicalKeyboardKey.keyP):
            const _NavIntent('/'),
        LogicalKeySet(LogicalKeyboardKey.control, LogicalKeyboardKey.keyH):
            const _NavIntent('/history'),
        LogicalKeySet(LogicalKeyboardKey.control, LogicalKeyboardKey.keyS):
            const _NavIntent('/stats'),
        LogicalKeySet(LogicalKeyboardKey.control, LogicalKeyboardKey.comma):
            const _NavIntent('/settings'),
        LogicalKeySet(LogicalKeyboardKey.control, LogicalKeyboardKey.keyL):
            const _NavIntent('/playlist'),
      },
      child: Actions(
        actions: {
          _NavIntent: CallbackAction<_NavIntent>(
            onInvoke: (intent) => context.go(intent.path),
          ),
        },
        child: Focus(
          autofocus: true,
          child: Scaffold(
            appBar: const PreferredSize(
              preferredSize: Size.fromHeight(56),
              child: TopBar(),
            ),
            body: Row(
              children: [
                const Sidebar(),
                const VerticalDivider(width: 1),
                Expanded(child: child),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _NavIntent extends Intent {
  final String path;
  const _NavIntent(this.path);
}
