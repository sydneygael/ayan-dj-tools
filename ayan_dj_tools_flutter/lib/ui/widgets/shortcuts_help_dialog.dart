import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';

class ShortcutsHelpDialog extends StatelessWidget {
  const ShortcutsHelpDialog({super.key});

  static void show(BuildContext context) {
    showDialog<void>(
      context: context,
      builder: (context) => const ShortcutsHelpDialog(),
    );
  }

  @override
  Widget build(BuildContext context) {
    final shortcuts = [
      ('Ctrl + P', 'toolbar.chat'.tr()),
      ('Ctrl + H', 'toolbar.history'.tr()),
      ('Ctrl + S', 'toolbar.stats'.tr()),
      ('Ctrl + L', 'Playlist'),
      ('Ctrl + ,', 'toolbar.settings'.tr()),
    ];

    return AlertDialog(
      title: Row(
        children: [
          const Icon(Icons.keyboard, size: 20, color: AppColors.primaryCyan),
          const SizedBox(width: 8),
          Text('shortcuts.title'.tr()),
        ],
      ),
      content: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 360),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: shortcuts
              .map((entry) => _ShortcutRow(
                    shortcut: entry.$1,
                    label: entry.$2,
                  ))
              .toList(),
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: Text('common.close'.tr()),
        ),
      ],
    );
  }
}

class _ShortcutRow extends StatelessWidget {
  final String shortcut;
  final String label;

  const _ShortcutRow({required this.shortcut, required this.label});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            decoration: BoxDecoration(
              color: AppColors.primaryCyan.withValues(alpha: 0.1),
              border: Border.all(
                  color: AppColors.primaryCyan.withValues(alpha: 0.3)),
              borderRadius: BorderRadius.circular(4),
            ),
            child: Text(
              shortcut,
              style: const TextStyle(
                fontFamily: 'monospace',
                fontSize: 12,
                fontWeight: FontWeight.w600,
                color: AppColors.primaryCyan,
              ),
            ),
          ),
          const SizedBox(width: 16),
          Text(label, style: const TextStyle(fontSize: 13)),
        ],
      ),
    );
  }
}
