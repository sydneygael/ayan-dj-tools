import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/utils/notification_service.dart';
import '../../../data/models/models.dart';
import '../../../data/repositories/plan_repository.dart';
import '../../../core/theme/app_colors.dart';
import '../../widgets/confirm_dialog.dart';
import 'apply_mode_view.dart';
import 'manual_mode_view.dart';
import 'operation_card.dart';
import 'plan_progress_widget.dart';
import 'plan_summary.dart';

class PlanReviewPage extends ConsumerStatefulWidget {
  final String planId;

  const PlanReviewPage({super.key, required this.planId});

  @override
  ConsumerState<PlanReviewPage> createState() => _PlanReviewPageState();
}

class _PlanReviewPageState extends ConsumerState<PlanReviewPage> {
  TaggingPlan? _plan;
  bool _loading = true;
  bool _executing = false;
  BatchApplyResult? _result;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadPlan();
  }

  Future<void> _loadPlan() async {
    setState(() => _loading = true);
    try {
      final plan =
          await ref.read(planRepositoryProvider).getPlan(widget.planId);
      if (mounted) {
        setState(() {
          _plan = plan;
          _error = null;
          _loading = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _error = 'plan.loadError'.tr();
          _loading = false;
        });
      }
    }
  }

  Future<void> _approveAll() async {
    try {
      final updated =
          await ref.read(planRepositoryProvider).approvePlan(widget.planId);
      setState(() => _plan = updated);
      NotificationService.show('plan.planApproved'.tr());
    } catch (_) {
      NotificationService.error('plan.approveError'.tr());
    }
  }

  Future<void> _execute() async {
    final confirmed = await ConfirmDialog.show(
      context,
      title: 'plan.executePlan'.tr(),
      message: 'plan.applyTagsConfirm'.tr(
          namedArgs: {'count': (_plan?.operations.length ?? 0).toString()}),
      confirmLabel: 'plan.execute'.tr(),
      warn: true,
    );
    if (!confirmed || !mounted) return;

    setState(() => _executing = true);
    try {
      final result =
          await ref.read(planRepositoryProvider).executePlan(widget.planId);
      setState(() {
        _result = result;
        _executing = false;
      });
      NotificationService.show('plan.executionDone'.tr(namedArgs: {
        'success': result.successCount.toString(),
        'total': result.totalOperations.toString(),
      }));
      await _loadPlan();
    } catch (_) {
      NotificationService.error('plan.executeError'.tr());
      if (mounted) setState(() => _executing = false);
    }
  }

  Future<void> _delete() async {
    final confirmed = await ConfirmDialog.show(
      context,
      title: 'plan.deletePlan'.tr(),
      message: 'plan.deleteIrreversible'.tr(),
      confirmLabel: 'common.delete'.tr(),
      warn: true,
    );
    if (!confirmed || !mounted) return;

    try {
      await ref.read(planRepositoryProvider).deletePlan(widget.planId);
      NotificationService.show('plan.deleted'.tr());
      if (mounted) context.go('/');
    } catch (_) {
      NotificationService.error('plan.deleteError'.tr());
    }
  }

  void _toggleOp(String filepath, bool approve) {
    final plan = _plan;
    if (plan == null) return;
    setState(() {
      _plan = TaggingPlan(
        planId: plan.planId,
        operations: plan.operations
            .map((op) => op.filepath == filepath
                ? op.copyWith(
                    status: approve
                        ? OperationStatus.approved
                        : OperationStatus.rejected)
                : op)
            .toList(),
        createdAt: plan.createdAt,
        status: plan.status,
        totalFiles: plan.totalFiles,
        filesWithMissingTags: plan.filesWithMissingTags,
        mode: plan.mode,
        currentIndex: plan.currentIndex,
      );
    });
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_error != null || _plan == null) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(_error ?? 'plan.notFound'.tr(),
                style: const TextStyle(color: AppColors.error)),
            const SizedBox(height: 16),
            ElevatedButton(
                onPressed: _loadPlan, child: Text('common.retry'.tr())),
          ],
        ),
      );
    }

    final plan = _plan!;

    // MANUAL mode — step by step
    if (plan.mode == OperatingMode.manual) {
      return SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            PlanSummary(plan: plan),
            ManualModeView(
              plan: plan,
              onCompleted: () => context.go('/history'),
            ),
            _DeleteButton(onTap: _delete),
          ],
        ),
      );
    }

    // APPLY mode — auto-execute with live log
    if (plan.mode == OperatingMode.apply) {
      return SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            PlanSummary(plan: plan),
            ApplyModeView(
              plan: plan,
              onCompleted: () {},
            ),
            _DeleteButton(onTap: _delete),
          ],
        ),
      );
    }

    // PLAN mode — full review
    final canApprove = plan.status == PlanStatus.draft ||
        plan.status == PlanStatus.readyForReview;
    final canExecute = plan.status == PlanStatus.approved;

    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          PlanSummary(plan: plan),

          // Action buttons
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
            child: Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                if (canApprove)
                  ElevatedButton.icon(
                    icon: const Icon(Icons.check_circle, size: 16),
                    label: Text('plan.approveAll'.tr()),
                    onPressed: _approveAll,
                  ),
                if (canExecute)
                  ElevatedButton.icon(
                    icon: _executing
                        ? const SizedBox(
                            width: 16,
                            height: 16,
                            child: CircularProgressIndicator(strokeWidth: 2))
                        : const Icon(Icons.play_arrow, size: 16),
                    label: Text('plan.execute'.tr()),
                    style: ElevatedButton.styleFrom(
                        backgroundColor: AppColors.success,
                        foregroundColor: Colors.white),
                    onPressed: _executing ? null : _execute,
                  ),
                OutlinedButton.icon(
                  icon: const Icon(Icons.delete, size: 16),
                  label: Text('common.delete'.tr()),
                  style: OutlinedButton.styleFrom(
                      foregroundColor: AppColors.error),
                  onPressed: _delete,
                ),
              ],
            ),
          ),

          PlanProgressWidget(
            result: _result,
            total: plan.operations.length,
            executing: _executing,
          ),

          // Operation cards
          ...plan.operations.map((op) => OperationCard(
                operation: op,
                onConfirm: plan.status == PlanStatus.draft ||
                        plan.status == PlanStatus.readyForReview
                    ? _toggleOp
                    : null,
              )),

          const SizedBox(height: 24),
        ],
      ),
    );
  }
}

class _DeleteButton extends StatelessWidget {
  final VoidCallback onTap;
  const _DeleteButton({required this.onTap});

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.all(16),
        child: OutlinedButton.icon(
          icon: const Icon(Icons.delete, size: 16),
          label: Text('common.delete'.tr()),
          style:
              OutlinedButton.styleFrom(foregroundColor: AppColors.error),
          onPressed: onTap,
        ),
      );
}
