import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/notification_service.dart';
import '../../data/repositories/plan_repository.dart';
import '../../providers/file_provider.dart';
import '../../providers/mode_provider.dart';
import '../widgets/drag_drop_zone.dart';
import '../widgets/audio_player_widget.dart';

class Sidebar extends ConsumerStatefulWidget {
  const Sidebar({super.key});

  @override
  ConsumerState<Sidebar> createState() => _SidebarState();
}

class _SidebarState extends ConsumerState<Sidebar> {
  bool _creatingPlan = false;

  Future<void> _createPlan() async {
    final files = ref.read(fileProvider);
    final mode = ref.read(modeProvider);
    if (files.isEmpty) return;

    setState(() => _creatingPlan = true);
    try {
      final plan = await ref
          .read(planRepositoryProvider)
          .createPlan(filePaths: files, mode: mode);
      if (mounted) {
        NotificationService.show(
          'sidebar.planCreated'.tr(namedArgs: {'planId': plan.planId}),
        );
        context.go('/plan/${plan.planId}');
      }
    } catch (_) {
      NotificationService.error('sidebar.planCreateError'.tr());
    } finally {
      if (mounted) setState(() => _creatingPlan = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final files = ref.watch(fileProvider);
    final selectedFile = ref.watch(selectedFileProvider);

    return SizedBox(
      width: AppColors.sidebarWidth,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Header
          Padding(
            padding: const EdgeInsets.fromLTRB(12, 8, 4, 8),
            child: Row(
              children: [
                Text(
                  'sidebar.files'.tr(),
                  style: const TextStyle(
                      fontWeight: FontWeight.w600, fontSize: 13),
                ),
                if (files.isNotEmpty) ...[
                  const SizedBox(width: 4),
                  Text(
                    'files.count'.tr(
                        namedArgs: {'count': files.length.toString()}),
                    style: const TextStyle(fontSize: 11, color: Colors.grey),
                  ),
                ],
                const Spacer(),
                if (files.isNotEmpty)
                  TextButton(
                    onPressed: () {
                      ref.read(fileProvider.notifier).clear();
                      ref.read(selectedFileProvider.notifier).state = null;
                    },
                    style: TextButton.styleFrom(
                        minimumSize: Size.zero,
                        padding: const EdgeInsets.symmetric(
                            horizontal: 8, vertical: 4)),
                    child: Text('sidebar.clear'.tr(),
                        style: const TextStyle(fontSize: 12)),
                  ),
              ],
            ),
          ),

          const Divider(height: 1),

          // File picker buttons
          Padding(
            padding: const EdgeInsets.all(8),
            child: Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    icon: const Icon(Icons.audio_file, size: 14),
                    label: Text(
                      'sidebar.selectFiles'.tr(),
                      style: const TextStyle(fontSize: 11),
                      overflow: TextOverflow.ellipsis,
                    ),
                    style: OutlinedButton.styleFrom(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 8, vertical: 6)),
                    onPressed: () =>
                        ref.read(fileProvider.notifier).pickFiles(),
                  ),
                ),
                const SizedBox(width: 4),
                OutlinedButton(
                  onPressed: () =>
                      ref.read(fileProvider.notifier).pickFolder(),
                  style: OutlinedButton.styleFrom(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 8, vertical: 6)),
                  child: Text('sidebar.selectFolder'.tr(),
                      style: const TextStyle(fontSize: 11)),
                ),
              ],
            ),
          ),

          // Drag & drop zone
          const DragDropZone(),
          const SizedBox(height: 4),

          // File list
          Expanded(
            child: files.isEmpty
                ? Center(
                    child: Text(
                      'files.noFilesSelected'.tr(),
                      style: TextStyle(
                          color: Colors.grey.shade500, fontSize: 13),
                    ),
                  )
                : ListView.builder(
                    padding: const EdgeInsets.symmetric(horizontal: 4),
                    itemCount: files.length,
                    itemBuilder: (context, i) {
                      final file = files[i];
                      final filename = file.split(RegExp(r'[/\\]')).last;
                      final isSelected = file == selectedFile;

                      return InkWell(
                        onTap: () => ref
                            .read(selectedFileProvider.notifier)
                            .state = isSelected ? null : file,
                        child: Container(
                          decoration: BoxDecoration(
                            color: isSelected
                                ? AppColors.primaryCyan.withValues(alpha: 0.1)
                                : null,
                            borderRadius: BorderRadius.circular(4),
                          ),
                          child: ListTile(
                            dense: true,
                            contentPadding:
                                const EdgeInsets.symmetric(horizontal: 8),
                            leading: Icon(
                              Icons.music_note,
                              size: 16,
                              color: isSelected
                                  ? AppColors.primaryCyan
                                  : Colors.grey,
                            ),
                            title: Text(
                              filename,
                              style: TextStyle(
                                fontSize: 12,
                                color: isSelected
                                    ? AppColors.primaryCyan
                                    : null,
                              ),
                              overflow: TextOverflow.ellipsis,
                            ),
                            trailing: IconButton(
                              icon: const Icon(Icons.close, size: 14),
                              tooltip: 'files.removeFile'.tr(),
                              padding: EdgeInsets.zero,
                              constraints: const BoxConstraints(
                                  minWidth: 24, minHeight: 24),
                              onPressed: () {
                                ref.read(fileProvider.notifier).remove(file);
                                if (isSelected) {
                                  ref
                                      .read(selectedFileProvider.notifier)
                                      .state = null;
                                }
                              },
                            ),
                          ),
                        ),
                      );
                    },
                  ),
          ),

          const Divider(height: 1),

          // Audio player (visible when a file is selected)
          if (selectedFile != null) ...[
            AudioPlayerWidget(key: ValueKey(selectedFile), filepath: selectedFile),
            const Divider(height: 1),
          ],

          // Create plan button
          Padding(
            padding: const EdgeInsets.all(8),
            child: ElevatedButton.icon(
              icon: _creatingPlan
                  ? const SizedBox(
                      width: 14,
                      height: 14,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Icon(Icons.playlist_add, size: 18),
              label: Text('sidebar.createPlan'.tr()),
              onPressed: (files.isEmpty || _creatingPlan) ? null : _createPlan,
            ),
          ),
        ],
      ),
    );
  }
}
