import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../providers/theme_provider.dart';
import '../../core/theme/app_colors.dart';
import '../widgets/mode_selector.dart';
import '../widgets/backend_status_chip.dart';
import '../widgets/shortcuts_help_dialog.dart';

class TopBar extends ConsumerWidget {
  const TopBar({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final isDark = ref.watch(themeProvider);

    return AppBar(
      leading: InkWell(
        onTap: () => context.go('/'),
        child: const Padding(
          padding: EdgeInsets.all(12),
          child: Icon(Icons.equalizer, color: AppColors.primaryCyan),
        ),
      ),
      title: GestureDetector(
        onTap: () => context.go('/'),
        child: Text(
          'toolbar.brand'.tr(),
          style: const TextStyle(
            color: AppColors.primaryCyan,
            fontWeight: FontWeight.bold,
            fontSize: 16,
          ),
        ),
      ),
      actions: [
        // Mode selector
        const ModeSelector(),
        const SizedBox(width: 8),

        // Backend status
        const BackendStatusChip(),
        const SizedBox(width: 8),

        // Stats
        IconButton(
          icon: const Icon(Icons.bar_chart),
          tooltip: 'toolbar.stats'.tr(),
          onPressed: () => context.go('/stats'),
        ),

        // Playlist
        IconButton(
          icon: const Icon(Icons.queue_music),
          tooltip: 'Playlist (Ctrl+L)',
          onPressed: () => context.go('/playlist'),
        ),

        // History
        IconButton(
          icon: const Icon(Icons.history),
          tooltip: 'toolbar.history'.tr(),
          onPressed: () => context.go('/history'),
        ),

        // Settings
        IconButton(
          icon: const Icon(Icons.settings),
          tooltip: 'toolbar.settings'.tr(),
          onPressed: () => context.go('/settings'),
        ),

        // Theme toggle
        IconButton(
          icon: Icon(isDark ? Icons.light_mode : Icons.dark_mode),
          tooltip: isDark ? 'toolbar.lightTheme'.tr() : 'toolbar.darkTheme'.tr(),
          onPressed: () => ref.read(themeProvider.notifier).toggle(),
        ),

        // Keyboard shortcuts help
        IconButton(
          icon: const Icon(Icons.keyboard, size: 20),
          tooltip: 'shortcuts.title'.tr(),
          onPressed: () => ShortcutsHelpDialog.show(context),
        ),

        const SizedBox(width: 8),
      ],
    );
  }
}
