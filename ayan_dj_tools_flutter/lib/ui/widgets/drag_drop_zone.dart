import 'package:desktop_drop/desktop_drop.dart';
import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/constants/audio_extensions.dart';
import '../../core/theme/app_colors.dart';
import '../../providers/file_provider.dart';

class DragDropZone extends ConsumerStatefulWidget {
  const DragDropZone({super.key});

  @override
  ConsumerState<DragDropZone> createState() => _DragDropZoneState();
}

class _DragDropZoneState extends ConsumerState<DragDropZone> {
  bool _dragging = false;

  @override
  Widget build(BuildContext context) {
    return DropTarget(
      onDragEntered: (_) => setState(() => _dragging = true),
      onDragExited: (_) => setState(() => _dragging = false),
      onDragDone: (detail) {
        setState(() => _dragging = false);
        final paths = detail.files
            .where((f) => AudioExtensions.isAudio(f.name))
            .map((f) => f.path)
            .toList();
        if (paths.isNotEmpty) {
          ref.read(fileProvider.notifier).addFiles(paths);
        }
      },
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        margin: const EdgeInsets.symmetric(horizontal: 8),
        padding: const EdgeInsets.symmetric(vertical: 12),
        decoration: BoxDecoration(
          border: Border.all(
            color: _dragging ? AppColors.primaryCyan : Colors.grey.shade700,
            width: 1.5,
            style: BorderStyle.solid,
          ),
          borderRadius: BorderRadius.circular(6),
          color: _dragging
              ? AppColors.primaryCyan.withValues(alpha: 0.08)
              : Colors.transparent,
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              Icons.cloud_upload_outlined,
              size: 24,
              color: _dragging ? AppColors.primaryCyan : Colors.grey.shade600,
            ),
            const SizedBox(height: 4),
            Text(
              'files.dragDrop'.tr(),
              style: TextStyle(
                fontSize: 11,
                color: _dragging ? AppColors.primaryCyan : Colors.grey.shade600,
              ),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }
}
