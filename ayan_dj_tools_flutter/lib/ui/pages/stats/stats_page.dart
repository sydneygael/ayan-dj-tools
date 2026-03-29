import 'package:easy_localization/easy_localization.dart';
import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_colors.dart';
import '../../../data/models/models.dart';
import '../../../data/repositories/stats_repository.dart';
import 'camelot_wheel.dart';

class StatsPage extends ConsumerStatefulWidget {
  const StatsPage({super.key});

  @override
  ConsumerState<StatsPage> createState() => _StatsPageState();
}

class _StatsPageState extends ConsumerState<StatsPage>
    with SingleTickerProviderStateMixin {
  late TabController _tabs;

  StatsReport? _stats;
  CollectionProfile? _collection;
  EnrichmentStats? _enrichment;
  ActivityTimeline? _activity;
  bool _loading = true;
  String? _error;
  String _period = 'week';

  @override
  void initState() {
    super.initState();
    _tabs = TabController(length: 3, vsync: this);
    _load();
  }

  @override
  void dispose() {
    _tabs.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final repo = ref.read(statsRepositoryProvider);
      final results = await Future.wait([
        repo.getStats(),
        repo.getCollection(),
        repo.getEnrichment(),
        repo.getActivity(period: _period),
      ]);
      if (mounted) {
        setState(() {
          _stats = results[0] as StatsReport;
          _collection = results[1] as CollectionProfile;
          _enrichment = results[2] as EnrichmentStats;
          _activity = results[3] as ActivityTimeline;
          _loading = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _error = 'stats.loadError'.tr();
          _loading = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_error != null) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(_error!, style: const TextStyle(color: AppColors.error)),
            ElevatedButton(onPressed: _load, child: Text('common.retry'.tr())),
          ],
        ),
      );
    }

    return Column(
      children: [
        // Header
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 0),
          child: Row(
            children: [
              Text('stats.title'.tr(),
                  style: const TextStyle(
                      fontSize: 20, fontWeight: FontWeight.bold)),
              const Spacer(),
              if (_stats != null) ...[
                _KpiChip(
                    label: 'stats.plansCreated'.tr(),
                    value: _stats!.totalPlansCreated.toString()),
                const SizedBox(width: 8),
                _KpiChip(
                    label: 'stats.tagsApplied'.tr(),
                    value: _stats!.totalTagsApplied.toString()),
                const SizedBox(width: 8),
                _KpiChip(
                    label: 'stats.filesEnriched'.tr(),
                    value: _stats!.totalFilesEnriched.toString()),
              ],
            ],
          ),
        ),
        TabBar(
          controller: _tabs,
          tabs: [
            Tab(text: 'stats.tabs.collection'.tr()),
            Tab(text: 'stats.tabs.enrichment'.tr()),
            Tab(text: 'stats.tabs.activity'.tr()),
          ],
        ),
        Expanded(
          child: TabBarView(
            controller: _tabs,
            children: [
              _CollectionTab(collection: _collection),
              _EnrichmentTab(enrichment: _enrichment),
              _ActivityTab(
                activity: _activity,
                period: _period,
                onPeriodChanged: (p) {
                  setState(() => _period = p);
                  _load();
                },
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _KpiChip extends StatelessWidget {
  final String label;
  final String value;

  const _KpiChip({required this.label, required this.value});

  @override
  Widget build(BuildContext context) => Column(
        children: [
          Text(value,
              style: const TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: AppColors.primaryCyan)),
          Text(label,
              style: const TextStyle(fontSize: 10, color: Colors.grey)),
        ],
      );
}

// ---------------------------------------------------------------------------
// Collection Tab
// ---------------------------------------------------------------------------

class _CollectionTab extends StatelessWidget {
  final CollectionProfile? collection;

  const _CollectionTab({required this.collection});

  @override
  Widget build(BuildContext context) {
    final col = collection;
    if (col == null) return const Center(child: CircularProgressIndicator());
    if (col.totalTracksScanned == 0) {
      return Center(child: Text('stats.collection.empty'.tr()));
    }

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // KPI row
          Row(
            children: [
              _StatCard(
                  label: 'stats.collection.tracksScanned'.tr(),
                  value: col.totalTracksScanned.toString()),
              const SizedBox(width: 8),
              _StatCard(
                  label: 'stats.collection.tracksEnriched'.tr(),
                  value: col.totalTracksEnriched.toString()),
              const SizedBox(width: 8),
              _StatCard(
                  label: 'stats.collection.completeTags'.tr(),
                  value: col.totalWithCompleteTags.toString()),
            ],
          ),
          const SizedBox(height: 16),

          // Genre donut
          if (col.genreDistribution.isNotEmpty) ...[
            Text('stats.collection.genreDistribution'.tr(),
                style: const TextStyle(
                    fontWeight: FontWeight.w600, fontSize: 13)),
            const SizedBox(height: 8),
            SizedBox(
              height: 200,
              child: _GenreDonut(data: col.genreDistribution),
            ),
            const SizedBox(height: 16),
          ],

          // BPM histogram
          if (col.bpmHistogram.isNotEmpty) ...[
            Text('stats.collection.bpmHistogram'.tr(),
                style: const TextStyle(
                    fontWeight: FontWeight.w600, fontSize: 13)),
            const SizedBox(height: 8),
            SizedBox(
              height: 160,
              child: _BpmHistogram(data: col.bpmHistogram),
            ),
            const SizedBox(height: 16),
          ],

          // Camelot Wheel
          if (col.keyDistribution.isNotEmpty) ...[
            Text('stats.collection.keyDistribution'.tr(),
                style: const TextStyle(
                    fontWeight: FontWeight.w600, fontSize: 13)),
            const SizedBox(height: 8),
            Center(
              child: SizedBox(
                width: 280,
                child: CamelotWheel(keyDistribution: col.keyDistribution),
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class _StatCard extends StatelessWidget {
  final String label;
  final String value;

  const _StatCard({required this.label, required this.value});

  @override
  Widget build(BuildContext context) => Expanded(
        child: Card(
          child: Padding(
            padding: const EdgeInsets.all(12),
            child: Column(
              children: [
                Text(value,
                    style: const TextStyle(
                        fontSize: 20,
                        fontWeight: FontWeight.bold,
                        color: AppColors.primaryCyan)),
                const SizedBox(height: 4),
                Text(label,
                    style:
                        const TextStyle(fontSize: 11, color: Colors.grey),
                    textAlign: TextAlign.center),
              ],
            ),
          ),
        ),
      );
}

class _GenreDonut extends StatelessWidget {
  final Map<String, int> data;

  const _GenreDonut({required this.data});

  @override
  Widget build(BuildContext context) {
    // Top 9 genres + Others
    final sorted = data.entries.toList()
      ..sort((a, b) => b.value.compareTo(a.value));
    final top = sorted.take(9).toList();
    final rest = sorted.skip(9).fold(0, (sum, e) => sum + e.value);

    final items = [
      ...top.map((e) => e),
      if (rest > 0) MapEntry('Others', rest),
    ];

    const colors = [
      Color(0xFF00BCD4),
      Color(0xFF7C4DFF),
      Color(0xFF4CAF50),
      Color(0xFFFF9800),
      Color(0xFFF44336),
      Color(0xFF9C27B0),
      Color(0xFF2196F3),
      Color(0xFFFF5722),
      Color(0xFF795548),
      Color(0xFF607D8B),
    ];

    return PieChart(
      PieChartData(
        centerSpaceRadius: 50,
        sections: items.asMap().entries.map((e) {
          final color = colors[e.key % colors.length];
          return PieChartSectionData(
            value: e.value.value.toDouble(),
            color: color,
            title: e.value.key,
            titleStyle:
                const TextStyle(fontSize: 10, color: Colors.white),
            radius: 60,
          );
        }).toList(),
      ),
    );
  }
}

class _BpmHistogram extends StatelessWidget {
  final Map<String, int> data;

  const _BpmHistogram({required this.data});

  @override
  Widget build(BuildContext context) {
    final sorted = data.entries.toList()
      ..sort((a, b) => a.key.compareTo(b.key));
    final max = sorted.fold(0.0, (m, e) => e.value > m ? e.value.toDouble() : m);

    return BarChart(
      BarChartData(
        maxY: max * 1.1,
        barTouchData: BarTouchData(enabled: false),
        titlesData: FlTitlesData(
          leftTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
          rightTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
          topTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
          bottomTitles: AxisTitles(
            sideTitles: SideTitles(
              showTitles: true,
              getTitlesWidget: (v, _) {
                final idx = v.toInt();
                if (idx < 0 || idx >= sorted.length) return const SizedBox();
                return Padding(
                  padding: const EdgeInsets.only(top: 4),
                  child: Text(sorted[idx].key,
                      style: const TextStyle(fontSize: 9)),
                );
              },
              reservedSize: 24,
            ),
          ),
        ),
        gridData: const FlGridData(show: false),
        borderData: FlBorderData(show: false),
        barGroups: sorted.asMap().entries.map((e) {
          return BarChartGroupData(
            x: e.key,
            barRods: [
              BarChartRodData(
                toY: e.value.value.toDouble(),
                color: AppColors.primaryCyan,
                width: 12,
                borderRadius: const BorderRadius.vertical(top: Radius.circular(3)),
              ),
            ],
          );
        }).toList(),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Enrichment Tab
// ---------------------------------------------------------------------------

class _EnrichmentTab extends StatelessWidget {
  final EnrichmentStats? enrichment;

  const _EnrichmentTab({required this.enrichment});

  @override
  Widget build(BuildContext context) {
    final e = enrichment;
    if (e == null) return const Center(child: CircularProgressIndicator());

    final matchPct = (e.spotifyMatchRate * 100).toStringAsFixed(1);
    final errorPct = (e.errorRate * 100).toStringAsFixed(1);

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              _StatCard(
                  label: 'stats.enrichment.matchRate'.tr(),
                  value: '$matchPct%'),
              const SizedBox(width: 8),
              _StatCard(
                  label: 'stats.enrichment.errorRate'.tr(),
                  value: '$errorPct%'),
              const SizedBox(width: 8),
              _StatCard(
                  label: 'stats.enrichment.totalEnriched'.tr(),
                  value: e.mostEnrichedTagTypes.values
                      .fold(0, (s, v) => s + v)
                      .toString()),
            ],
          ),
          const SizedBox(height: 16),

          if (e.mostEnrichedTagTypes.isNotEmpty) ...[
            Text('stats.enrichment.tagTypes'.tr(),
                style: const TextStyle(
                    fontWeight: FontWeight.w600, fontSize: 13)),
            const SizedBox(height: 8),
            ...e.mostEnrichedTagTypes.entries.map((entry) => Padding(
                  padding: const EdgeInsets.symmetric(vertical: 2),
                  child: Row(
                    children: [
                      SizedBox(
                          width: 80,
                          child: Text(entry.key,
                              style: const TextStyle(fontSize: 12))),
                      Expanded(
                        child: LinearProgressIndicator(
                          value: e.mostEnrichedTagTypes.values.isEmpty
                              ? 0
                              : entry.value /
                                  e.mostEnrichedTagTypes.values.reduce(
                                      (a, b) => a > b ? a : b),
                          backgroundColor: Colors.grey.shade800,
                          valueColor: const AlwaysStoppedAnimation(
                              AppColors.primaryCyan),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Text(entry.value.toString(),
                          style: const TextStyle(fontSize: 12)),
                    ],
                  ),
                )),
          ],
        ],
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Activity Tab
// ---------------------------------------------------------------------------

class _ActivityTab extends StatelessWidget {
  final ActivityTimeline? activity;
  final String period;
  final void Function(String) onPeriodChanged;

  const _ActivityTab({
    required this.activity,
    required this.period,
    required this.onPeriodChanged,
  });

  @override
  Widget build(BuildContext context) {
    final a = activity;
    if (a == null) return const Center(child: CircularProgressIndicator());

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Period selector
          SegmentedButton<String>(
            segments: [
              ButtonSegment(
                  value: 'week',
                  label: Text('stats.activity.week'.tr())),
              ButtonSegment(
                  value: 'month',
                  label: Text('stats.activity.month'.tr())),
              ButtonSegment(
                  value: 'all',
                  label: Text('stats.activity.all'.tr())),
            ],
            selected: {period},
            onSelectionChanged: (s) {
              if (s.isNotEmpty) onPeriodChanged(s.first);
            },
          ),
          const SizedBox(height: 16),

          if (a.tagsAppliedPerPeriod.isNotEmpty) ...[
            Text('stats.activity.tagsApplied'.tr(),
                style: const TextStyle(
                    fontWeight: FontWeight.w600, fontSize: 13)),
            const SizedBox(height: 8),
            SizedBox(
              height: 160,
              child: _ActivityLineChart(data: a.tagsAppliedPerPeriod),
            ),
            const SizedBox(height: 16),
          ],

          if (a.modeUsage.isNotEmpty) ...[
            Text('stats.activity.modeUsage'.tr(),
                style: const TextStyle(
                    fontWeight: FontWeight.w600, fontSize: 13)),
            const SizedBox(height: 8),
            ...a.modeUsage.entries.map((e) => ListTile(
                  dense: true,
                  title: Text(e.key, style: const TextStyle(fontSize: 13)),
                  trailing: Text(e.value.toString(),
                      style: const TextStyle(
                          color: AppColors.primaryCyan,
                          fontWeight: FontWeight.w600)),
                )),
          ],
        ],
      ),
    );
  }
}

class _ActivityLineChart extends StatelessWidget {
  final Map<String, int> data;

  const _ActivityLineChart({required this.data});

  @override
  Widget build(BuildContext context) {
    final entries = data.entries.toList()
      ..sort((a, b) => a.key.compareTo(b.key));
    final spots = entries.asMap().entries.map((e) {
      return FlSpot(e.key.toDouble(), e.value.value.toDouble());
    }).toList();

    return LineChart(
      LineChartData(
        lineBarsData: [
          LineChartBarData(
            spots: spots,
            isCurved: true,
            color: AppColors.primaryCyan,
            barWidth: 2,
            dotData: const FlDotData(show: false),
            belowBarData: BarAreaData(
              show: true,
              color: AppColors.primaryCyan.withValues(alpha: 0.1),
            ),
          ),
        ],
        titlesData: FlTitlesData(
          leftTitles: const AxisTitles(
              sideTitles: SideTitles(showTitles: false)),
          rightTitles: const AxisTitles(
              sideTitles: SideTitles(showTitles: false)),
          topTitles: const AxisTitles(
              sideTitles: SideTitles(showTitles: false)),
          bottomTitles: AxisTitles(
            sideTitles: SideTitles(
              showTitles: true,
              getTitlesWidget: (v, _) {
                final idx = v.toInt();
                if (idx < 0 || idx >= entries.length) return const SizedBox();
                final label = entries[idx].key;
                return Padding(
                  padding: const EdgeInsets.only(top: 4),
                  child: Text(
                    label.length > 5 ? label.substring(label.length - 5) : label,
                    style: const TextStyle(fontSize: 9),
                  ),
                );
              },
              reservedSize: 24,
            ),
          ),
        ),
        gridData: const FlGridData(show: false),
        borderData: FlBorderData(show: false),
      ),
    );
  }
}
