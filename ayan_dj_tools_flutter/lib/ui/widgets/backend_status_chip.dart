import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../providers/backend_status_provider.dart';
import '../../core/theme/app_colors.dart';

class BackendStatusChip extends ConsumerWidget {
  const BackendStatusChip({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final statusAsync = ref.watch(backendStatusProvider);
    final isReady = statusAsync.valueOrNull ?? false;

    return Chip(
      avatar: Icon(
        Icons.circle,
        size: 10,
        color: isReady ? AppColors.success : AppColors.error,
      ),
      label: Text(
        isReady ? 'backend.ready'.tr() : 'backend.notReady'.tr(),
        style: const TextStyle(fontSize: 11),
      ),
      padding: const EdgeInsets.symmetric(horizontal: 4),
      materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
    );
  }
}
