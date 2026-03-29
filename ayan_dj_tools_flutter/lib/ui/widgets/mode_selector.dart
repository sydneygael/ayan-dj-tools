import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../data/models/models.dart';
import '../../providers/mode_provider.dart';
import '../../core/theme/app_colors.dart';

class ModeSelector extends ConsumerWidget {
  const ModeSelector({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final current = ref.watch(modeProvider);
    return SegmentedButton<OperatingMode>(
      style: SegmentedButton.styleFrom(
        selectedBackgroundColor: AppColors.primaryCyan.withValues(alpha: 0.2),
        selectedForegroundColor: AppColors.primaryCyan,
        side: const BorderSide(color: AppColors.primaryCyan, width: 0.5),
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
        textStyle: const TextStyle(fontSize: 12),
      ),
      segments: [
        ButtonSegment(
          value: OperatingMode.plan,
          label: Text('mode.plan'.tr()),
          tooltip: 'mode.plan'.tr(),
        ),
        ButtonSegment(
          value: OperatingMode.manual,
          label: Text('mode.manual'.tr()),
          tooltip: 'mode.manual'.tr(),
        ),
        ButtonSegment(
          value: OperatingMode.apply,
          label: Text('mode.apply'.tr()),
          tooltip: 'mode.apply'.tr(),
        ),
      ],
      selected: {current},
      onSelectionChanged: (modes) {
        if (modes.isNotEmpty) {
          ref.read(modeProvider.notifier).setMode(modes.first);
        }
      },
    );
  }
}
