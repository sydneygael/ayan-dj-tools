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
import 'plan_progress_widget.dart';

class ApplyModeView extends ConsumerStatefulWidget {
  final TaggingPlan plan;
  final VoidCallback onCompleted;

  const ApplyModeView({
    super.key,
    required this.plan,
    required this.onCompleted,
  });

  @override
  ConsumerState<ApplyModeView> createState() => _ApplyModeViewState();
}

class _ApplyModeViewState extends ConsumerState<ApplyModeView> {
  final List<TagProgressEvent> _events = [];
  BatchApplyResult? _result;
  bool _executing = false;
  late StompPlanService _stomp;
  StreamSubscription<TagProgressEvent>? _sub;
  final _scrollController = ScrollController();

  @override
  void initState() {
    super.initState();
    _stomp = StompPlanService(ref.read(settingsProvider).wsUrl);
    _stomp.connect(widget.plan.planId);
    _sub = _stomp.eventStream.listen(_handleProgress);
    _start();
  }

  @override
  void dispose() {
    _sub?.cancel();
    _stomp.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  void _handleProgress(TagProgressEvent event) {
    if (!mounted) return;
    setState(() => _events.add(event));
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scrollController.hasClients) {
        _scrollController.animateTo(
          _scrollController.position.maxScrollExtent,
          duration: const Duration(milliseconds: 100),
          curve: Curves.easeOut,
        );
      }
    });
    if (event.index >= event.total - 1) {
      setState(() => _executing = false);
      NotificationService.show('apply.executionComplete'.tr());
      widget.onCompleted();
    }
  }

  Future<void> _start() async {
    setState(() => _executing = true);
    try {
      await ref.read(planRepositoryProvider).autoExecute(widget.plan.planId);
    } catch (_) {
      NotificationService.error('apply.autoExecuteError'.tr());
      if (mounted) setState(() => _executing = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final total = widget.plan.operations.length;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          child: Row(
            children: [
              const Icon(Icons.flash_auto, size: 16, color: AppColors.primaryCyan),
              const SizedBox(width: 8),
              Text(
                _executing
                    ? 'apply.autoExecuting'.tr()
                    : 'apply.executionComplete'.tr(),
                style: const TextStyle(fontSize: 13),
              ),
            ],
          ),
        ),

        PlanProgressWidget(
          result: _result,
          total: total,
          executing: _executing,
        ),

        // Live log
        if (_events.isNotEmpty)
          Card(
            margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
            child: SizedBox(
              height: 300,
              child: ListView.builder(
                controller: _scrollController,
                padding: const EdgeInsets.all(8),
                itemCount: _events.length,
                itemBuilder: (context, i) {
                  final e = _events[i];
                  final filename = e.filepath.split(RegExp(r'[/\\]')).last;
                  final isError = e.status == OperationStatus.error;
                  return Padding(
                    padding: const EdgeInsets.symmetric(vertical: 2),
                    child: Row(
                      children: [
                        Icon(
                          isError ? Icons.error_outline : Icons.check_circle_outline,
                          size: 14,
                          color: isError ? AppColors.error : AppColors.success,
                        ),
                        const SizedBox(width: 6),
                        Expanded(
                          child: Text(
                            '[${e.index + 1}/${e.total}] $filename — ${e.message}',
                            style: TextStyle(
                              fontSize: 11,
                              fontFamily: 'monospace',
                              color: isError ? AppColors.tagRemoved : null,
                            ),
                          ),
                        ),
                      ],
                    ),
                  );
                },
              ),
            ),
          ),
      ],
    );
  }
}
