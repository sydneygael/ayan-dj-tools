import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../../core/theme/app_colors.dart';
import '../../../data/models/models.dart';

class PlanSummary extends StatelessWidget {
  final TaggingPlan plan;

  const PlanSummary({super.key, required this.plan});

  @override
  Widget build(BuildContext context) {
    final statusLabel = switch (plan.status) {
      PlanStatus.draft => 'plan.status.draft',
      PlanStatus.readyForReview => 'plan.status.ready_for_review',
      PlanStatus.approved => 'plan.status.approved',
      PlanStatus.applying => 'plan.status.applying',
      PlanStatus.completed => 'plan.status.completed',
    };

    final statusColor = switch (plan.status) {
      PlanStatus.completed => AppColors.success,
      PlanStatus.applying => AppColors.info,
      PlanStatus.approved => AppColors.primaryCyan,
      _ => Colors.grey,
    };

    return Card(
      margin: const EdgeInsets.all(16),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    'Plan ${plan.planId.length > 8 ? plan.planId.substring(0, 8) : plan.planId}...',
                    style: const TextStyle(
                        fontWeight: FontWeight.w600, fontSize: 16),
                  ),
                ),
                Tooltip(
                  message: 'plan.copyId'.tr(),
                  child: IconButton(
                    icon: const Icon(Icons.copy, size: 16),
                    onPressed: () {
                      Clipboard.setData(ClipboardData(text: plan.planId));
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(content: Text('plan.copied'.tr())),
                      );
                    },
                  ),
                ),
                Chip(
                  label: Text(statusLabel.tr(),
                      style: const TextStyle(fontSize: 12)),
                  backgroundColor: statusColor.withValues(alpha: 0.15),
                  side: BorderSide(color: statusColor),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Wrap(
              spacing: 16,
              runSpacing: 4,
              children: [
                _InfoItem(
                  icon: Icons.folder,
                  label:
                      'plan.files'.tr(namedArgs: {'count': plan.totalFiles.toString()}),
                ),
                _InfoItem(
                  icon: Icons.label_off,
                  label:
                      '${plan.filesWithMissingTags} ${'plan.withMissingTags'.tr()}',
                ),
                _InfoItem(
                  icon: Icons.list,
                  label: '${plan.operations.length} ${'plan.operations'.tr()}',
                ),
                _InfoItem(
                  icon: Icons.calendar_today,
                  label: 'plan.createdAt'.tr(namedArgs: {
                    'date': plan.createdAt.split('T').first,
                  }),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _InfoItem extends StatelessWidget {
  final IconData icon;
  final String label;

  const _InfoItem({required this.icon, required this.label});

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, size: 14, color: Colors.grey),
        const SizedBox(width: 4),
        Text(label, style: const TextStyle(fontSize: 12, color: Colors.grey)),
      ],
    );
  }
}
