import 'dart:async';

import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/utils/notification_service.dart';
import '../../../data/models/models.dart';
import '../../../data/repositories/plan_repository.dart';
import '../../../data/services/stomp_plan_service.dart';
import '../../../providers/settings_provider.dart';
import 'operation_card.dart';

class ManualModeView extends ConsumerStatefulWidget {
  final TaggingPlan plan;
  final VoidCallback onCompleted;

  const ManualModeView({
    super.key,
    required this.plan,
    required this.onCompleted,
  });

  @override
  ConsumerState<ManualModeView> createState() => _ManualModeViewState();
}

class _ManualModeViewState extends ConsumerState<ManualModeView> {
  TagOperation? _currentOp;
  bool _loading = true;
  bool _done = false;
  late StompPlanService _stomp;
  StreamSubscription<TagProgressEvent>? _sub;

  @override
  void initState() {
    super.initState();
    _stomp = StompPlanService(ref.read(settingsProvider).wsUrl);
    _stomp.connect(widget.plan.planId);
    _sub = _stomp.eventStream.listen(_handleProgress);
    _loadCurrentOp();
  }

  @override
  void dispose() {
    _sub?.cancel();
    _stomp.dispose();
    super.dispose();
  }

  void _handleProgress(TagProgressEvent event) {
    if (event.index >= event.total - 1) {
      setState(() => _done = true);
      NotificationService.show('manual.processingComplete'.tr());
      widget.onCompleted();
    }
  }

  Future<void> _loadCurrentOp() async {
    setState(() => _loading = true);
    try {
      final op = await ref
          .read(planRepositoryProvider)
          .getCurrentOperation(widget.plan.planId);
      if (mounted) {
        setState(() {
          _currentOp = op;
          _done = op == null;
          _loading = false;
        });
      }
    } catch (_) {
      NotificationService.error('manual.loadError'.tr());
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _confirm(bool approve) async {
    final op = _currentOp;
    if (op == null) return;

    final index = widget.plan.operations.indexWhere(
        (o) => o.filepath == op.filepath);
    if (index < 0) return;

    try {
      await ref
          .read(planRepositoryProvider)
          .confirmOperation(widget.plan.planId, index, approve);
      NotificationService.show(
        approve
            ? 'manual.operationApproved'.tr()
            : 'manual.operationRejected'.tr(),
      );
      await _loadCurrentOp();
    } catch (_) {
      NotificationService.error('manual.confirmError'.tr());
    }
  }

  @override
  Widget build(BuildContext context) {
    final current = widget.plan.currentIndex;
    final total = widget.plan.operations.length;

    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_done || _currentOp == null) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.check_circle, size: 48, color: AppColors.success),
            const SizedBox(height: 16),
            Text('manual.processingComplete'.tr(),
                style: const TextStyle(fontSize: 18)),
          ],
        ),
      );
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          child: Row(
            children: [
              Text(
                'manual.fileProgress'.tr(
                    namedArgs: {
                      'current': (current + 1).toString(),
                      'total': total.toString(),
                    }),
                style: const TextStyle(fontSize: 13, color: Colors.grey),
              ),
              const Spacer(),
              SizedBox(
                width: 200,
                child: LinearProgressIndicator(
                  value: total > 0 ? (current + 1) / total : 0,
                  backgroundColor: Colors.grey.shade800,
                  valueColor: const AlwaysStoppedAnimation(AppColors.primaryCyan),
                ),
              ),
            ],
          ),
        ),
        OperationCard(
          operation: _currentOp!,
          onConfirm: (_, approve) => _confirm(approve),
        ),
      ],
    );
  }
}
