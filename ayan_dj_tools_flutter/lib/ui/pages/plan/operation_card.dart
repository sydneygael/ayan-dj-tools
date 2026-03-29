import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../data/models/models.dart';

class OperationCard extends StatelessWidget {
  final TagOperation operation;
  final void Function(String filepath, bool approve)? onConfirm;

  const OperationCard({
    super.key,
    required this.operation,
    this.onConfirm,
  });

  @override
  Widget build(BuildContext context) {
    final filename = operation.filepath.split(RegExp(r'[/\\]')).last;
    final statusColor = switch (operation.status) {
      OperationStatus.approved => AppColors.success,
      OperationStatus.rejected => AppColors.error,
      OperationStatus.applied => AppColors.primaryCyan,
      OperationStatus.error => AppColors.error,
      OperationStatus.pending => Colors.grey,
    };

    // Compute changed tags (keys present in both or only in suggested)
    final allKeys = {
      ...operation.currentTags.keys,
      ...operation.suggestedTags.keys,
    };
    final changedKeys = allKeys.where((k) =>
        operation.suggestedTags.containsKey(k) &&
        operation.currentTags[k] != operation.suggestedTags[k]).toList();

    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      child: ExpansionTile(
        tilePadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
        title: Row(
          children: [
            Expanded(
              child: Text(filename,
                  style: const TextStyle(fontSize: 13),
                  overflow: TextOverflow.ellipsis),
            ),
            Chip(
              label: Text(
                switch (operation.status) {
                  OperationStatus.approved => 'plan.status.approved',
                  OperationStatus.rejected => 'plan.reject',
                  OperationStatus.applied => 'plan.status.completed',
                  OperationStatus.error => 'errors.unexpected',
                  OperationStatus.pending => 'plan.tag',
                }
                    .tr(),
                style: const TextStyle(fontSize: 11),
              ),
              backgroundColor: statusColor.withValues(alpha: 0.15),
              side: BorderSide(color: statusColor),
              materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
            ),
          ],
        ),
        subtitle: Text(
          operation.filepath,
          style: const TextStyle(fontSize: 11, color: Colors.grey),
          overflow: TextOverflow.ellipsis,
        ),
        children: [
          // Diff table
          if (changedKeys.isNotEmpty)
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
              child: Table(
                border: TableBorder.all(
                  color: Colors.grey.shade800,
                  width: 0.5,
                ),
                columnWidths: const {
                  0: FlexColumnWidth(1.5),
                  1: FlexColumnWidth(2),
                  2: FlexColumnWidth(2),
                },
                children: [
                  TableRow(
                    decoration: BoxDecoration(
                      color: Colors.grey.shade900,
                    ),
                    children: [
                      _HeaderCell('plan.tag'.tr()),
                      _HeaderCell('plan.current'.tr()),
                      _HeaderCell('plan.suggested'.tr()),
                    ],
                  ),
                  ...changedKeys.map((key) {
                    final current = operation.currentTags[key] ?? '';
                    final suggested = operation.suggestedTags[key] ?? '';
                    final changed = current != suggested;

                    return TableRow(
                      children: [
                        _DataCell(key, style: const TextStyle(fontSize: 12)),
                        _DataCell(
                          current.isEmpty ? '—' : current,
                          style: TextStyle(
                            fontSize: 12,
                            color: changed
                                ? AppColors.tagRemoved
                                : null,
                            decoration: changed && current.isNotEmpty
                                ? TextDecoration.lineThrough
                                : null,
                          ),
                        ),
                        _DataCell(
                          suggested.isEmpty ? '—' : suggested,
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: changed ? FontWeight.w600 : null,
                            color: changed ? AppColors.tagAdded : null,
                          ),
                        ),
                      ],
                    );
                  }),
                ],
              ),
            ),

          // Approve / Reject buttons (visible for PENDING operations)
          if (onConfirm != null &&
              operation.status == OperationStatus.pending) ...[
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
              child: Row(
                children: [
                  OutlinedButton.icon(
                    icon: const Icon(Icons.close, size: 16),
                    label: Text('plan.reject'.tr()),
                    style: OutlinedButton.styleFrom(
                        foregroundColor: AppColors.error),
                    onPressed: () =>
                        onConfirm!(operation.filepath, false),
                  ),
                  const SizedBox(width: 8),
                  ElevatedButton.icon(
                    icon: const Icon(Icons.check, size: 16),
                    label: Text('plan.approve'.tr()),
                    onPressed: () =>
                        onConfirm!(operation.filepath, true),
                  ),
                ],
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class _HeaderCell extends StatelessWidget {
  final String text;
  const _HeaderCell(this.text);

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
        child: Text(text,
            style: const TextStyle(
                fontSize: 11, fontWeight: FontWeight.w600)),
      );
}

class _DataCell extends StatelessWidget {
  final String text;
  final TextStyle? style;
  const _DataCell(this.text, {this.style});

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
        child: Text(text, style: style ?? const TextStyle(fontSize: 12)),
      );
}
