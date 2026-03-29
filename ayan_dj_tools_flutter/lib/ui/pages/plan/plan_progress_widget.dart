import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../data/models/models.dart';

class PlanProgressWidget extends StatelessWidget {
  final BatchApplyResult? result;
  final int total;
  final bool executing;

  const PlanProgressWidget({
    super.key,
    required this.result,
    required this.total,
    required this.executing,
  });

  @override
  Widget build(BuildContext context) {
    if (!executing && result == null) return const SizedBox.shrink();

    final res = result;
    final progress = res != null
        ? (res.successCount + res.errorCount) / total.clamp(1, double.infinity)
        : null;

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              LinearProgressIndicator(
                value: executing ? null : progress,
                backgroundColor: Colors.grey.shade800,
                valueColor: const AlwaysStoppedAnimation(AppColors.primaryCyan),
              ),
              if (res != null) ...[
                const SizedBox(height: 8),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceAround,
                  children: [
                    _Stat(
                      label: 'common.total'.tr(),
                      value: res.totalOperations.toString(),
                      color: Colors.grey,
                    ),
                    _Stat(
                      label: 'common.success'.tr(),
                      value: res.successCount.toString(),
                      color: AppColors.success,
                    ),
                    _Stat(
                      label: 'plan.failures'.tr(),
                      value: res.errorCount.toString(),
                      color: res.errorCount > 0 ? AppColors.error : Colors.grey,
                    ),
                  ],
                ),
                if (res.errorCount > 0) ...[
                  const SizedBox(height: 8),
                  TextButton.icon(
                    icon: const Icon(Icons.history, size: 16),
                    label: Text('common.viewHistory'.tr()),
                    onPressed: () {},
                  ),
                ],
              ],
            ],
          ),
        ),
      ),
    );
  }
}

class _Stat extends StatelessWidget {
  final String label;
  final String value;
  final Color color;

  const _Stat(
      {required this.label, required this.value, required this.color});

  @override
  Widget build(BuildContext context) => Column(
        children: [
          Text(value,
              style: TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                  color: color)),
          Text(label, style: const TextStyle(fontSize: 11, color: Colors.grey)),
        ],
      );
}
