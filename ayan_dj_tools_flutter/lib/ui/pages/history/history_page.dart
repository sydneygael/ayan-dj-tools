import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_colors.dart';
import '../../../data/models/models.dart';
import '../../../data/repositories/plan_repository.dart';

class HistoryPage extends ConsumerStatefulWidget {
  final String? initialPlanId;

  const HistoryPage({super.key, this.initialPlanId});

  @override
  ConsumerState<HistoryPage> createState() => _HistoryPageState();
}

class _HistoryPageState extends ConsumerState<HistoryPage> {
  late TextEditingController _searchController;
  List<TaggingHistoryEntry>? _entries;
  bool _loading = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _searchController = TextEditingController(text: widget.initialPlanId ?? '');
    if (widget.initialPlanId != null && widget.initialPlanId!.isNotEmpty) {
      WidgetsBinding.instance.addPostFrameCallback((_) => _search());
    }
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _search() async {
    final planId = _searchController.text.trim();
    if (planId.isEmpty) return;

    setState(() {
      _loading = true;
      _error = null;
      _entries = null;
    });

    try {
      final entries =
          await ref.read(planRepositoryProvider).getPlanHistory(planId);
      if (mounted) setState(() => _entries = entries);
    } catch (_) {
      if (mounted) setState(() => _error = 'errors.network'.tr());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Search bar
          Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _searchController,
                    decoration: InputDecoration(
                      labelText: 'history.planId'.tr(),
                      hintText: 'plan-xxxx',
                      prefixIcon: const Icon(Icons.search, size: 18),
                      contentPadding: const EdgeInsets.symmetric(
                          horizontal: 12, vertical: 10),
                    ),
                    onSubmitted: (_) => _search(),
                  ),
                ),
                const SizedBox(width: 12),
                ElevatedButton(
                  onPressed: _loading ? null : _search,
                  child: Text('history.search'.tr()),
                ),
              ],
            ),
          ),

          // Content
          Expanded(
            child: _buildContent(),
          ),
        ],
      ),
    );
  }

  Widget _buildContent() {
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_error != null) {
      return Center(
        child: Text(_error!, style: const TextStyle(color: AppColors.error)),
      );
    }

    if (_entries == null) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.history, size: 48, color: Colors.grey),
            const SizedBox(height: 12),
            Text('history.title'.tr(),
                style: const TextStyle(fontSize: 16, color: Colors.grey)),
          ],
        ),
      );
    }

    if (_entries!.isEmpty) {
      return Center(
        child: Text('history.noResults'.tr(),
            style: const TextStyle(color: Colors.grey)),
      );
    }

    return ListView.builder(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      itemCount: _entries!.length,
      itemBuilder: (context, i) => _HistoryEntryTile(entry: _entries![i]),
    );
  }
}

class _HistoryEntryTile extends StatelessWidget {
  final TaggingHistoryEntry entry;

  const _HistoryEntryTile({required this.entry});

  @override
  Widget build(BuildContext context) {
    final filename = entry.filepath.split(RegExp(r'[/\\]')).last;
    final date = entry.appliedAt.length > 10
        ? entry.appliedAt.substring(0, 10)
        : entry.appliedAt;

    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: ExpansionTile(
        leading: Icon(
          entry.success ? Icons.check_circle : Icons.error,
          color: entry.success ? AppColors.success : AppColors.error,
          size: 18,
        ),
        title: Text(
          filename,
          style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w500),
          overflow: TextOverflow.ellipsis,
        ),
        subtitle: Text(
          '$date — ${'history.changesCount'.tr(namedArgs: {'count': entry.changes.length.toString()})}',
          style: const TextStyle(fontSize: 11),
        ),
        childrenPadding:
            const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        children: [
          if (entry.changes.isEmpty)
            Text('history.empty'.tr(),
                style: const TextStyle(color: Colors.grey, fontSize: 12))
          else
            Table(
              columnWidths: const {
                0: FlexColumnWidth(2),
                1: FlexColumnWidth(3),
                2: FlexColumnWidth(3),
              },
              children: [
                _tableHeader(context),
                ...entry.changes.map((c) => _tableRow(c)),
              ],
            ),
        ],
      ),
    );
  }

  TableRow _tableHeader(BuildContext context) {
    final style = TextStyle(
        fontWeight: FontWeight.w600,
        fontSize: 11,
        color: Theme.of(context).colorScheme.onSurfaceVariant);
    return TableRow(children: [
      Padding(
          padding: const EdgeInsets.only(bottom: 4),
          child: Text('history.changes'.tr(), style: style)),
      Padding(
          padding: const EdgeInsets.only(bottom: 4),
          child: Text('before', style: style)),
      Padding(
          padding: const EdgeInsets.only(bottom: 4),
          child: Text('after', style: style)),
    ]);
  }

  TableRow _tableRow(TagChange change) {
    return TableRow(children: [
      Padding(
        padding: const EdgeInsets.symmetric(vertical: 2),
        child: Text(change.tagName,
            style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w500)),
      ),
      Padding(
        padding: const EdgeInsets.symmetric(vertical: 2),
        child: Text(
          change.oldValue ?? '—',
          style: TextStyle(
              fontSize: 11,
              color: AppColors.tagRemoved,
              decoration: TextDecoration.lineThrough),
        ),
      ),
      Padding(
        padding: const EdgeInsets.symmetric(vertical: 2),
        child: Text(
          change.newValue,
          style: const TextStyle(
              fontSize: 11,
              color: AppColors.tagAdded,
              fontWeight: FontWeight.w600),
        ),
      ),
    ]);
  }
}
