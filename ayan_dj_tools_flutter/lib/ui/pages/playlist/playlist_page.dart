import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_colors.dart';
import '../../../data/models/models.dart';
import '../../../data/repositories/playlist_repository.dart';

class PlaylistPage extends ConsumerStatefulWidget {
  const PlaylistPage({super.key});

  @override
  ConsumerState<PlaylistPage> createState() => _PlaylistPageState();
}

class _PlaylistPageState extends ConsumerState<PlaylistPage> {
  final _bpmMinController = TextEditingController(text: '120');
  final _bpmMaxController = TextEditingController(text: '145');
  final _genreController = TextEditingController();

  Playlist? _playlist;
  bool _loading = false;
  String? _error;

  @override
  void dispose() {
    _bpmMinController.dispose();
    _bpmMaxController.dispose();
    _genreController.dispose();
    super.dispose();
  }

  Future<void> _generate() async {
    final bpmMin = int.tryParse(_bpmMinController.text.trim()) ?? 120;
    final bpmMax = int.tryParse(_bpmMaxController.text.trim()) ?? 145;
    final genre = _genreController.text.trim();

    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final pl = await ref.read(playlistRepositoryProvider).generate(
            bpmMin: bpmMin,
            bpmMax: bpmMax,
            genre: genre,
          );
      if (mounted) setState(() => _playlist = pl);
    } catch (_) {
      if (mounted) setState(() => _error = 'playlist.generateError'.tr());
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
          // Filter bar
          Padding(
            padding: const EdgeInsets.all(16),
            child: Wrap(
              spacing: 12,
              runSpacing: 8,
              crossAxisAlignment: WrapCrossAlignment.center,
              children: [
                _FilterField(
                  label: 'playlist.bpmMin'.tr(),
                  controller: _bpmMinController,
                  width: 100,
                  isNumber: true,
                ),
                _FilterField(
                  label: 'playlist.bpmMax'.tr(),
                  controller: _bpmMaxController,
                  width: 100,
                  isNumber: true,
                ),
                _FilterField(
                  label: 'playlist.genre'.tr(),
                  controller: _genreController,
                  width: 160,
                ),
                ElevatedButton.icon(
                  icon: _loading
                      ? const SizedBox(
                          width: 14,
                          height: 14,
                          child: CircularProgressIndicator(strokeWidth: 2))
                      : const Icon(Icons.auto_awesome, size: 16),
                  label: Text('playlist.generate'.tr()),
                  onPressed: _loading ? null : _generate,
                ),
              ],
            ),
          ),

          if (_error != null)
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: Text(_error!,
                  style: const TextStyle(color: AppColors.error, fontSize: 13)),
            ),

          // Track list
          Expanded(child: _buildContent()),
        ],
      ),
    );
  }

  Widget _buildContent() {
    if (_loading) return const Center(child: CircularProgressIndicator());

    if (_playlist == null) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.queue_music, size: 48, color: Colors.grey),
            const SizedBox(height: 12),
            Text('playlist.title'.tr(),
                style: const TextStyle(fontSize: 16, color: Colors.grey)),
          ],
        ),
      );
    }

    if (_playlist!.tracks.isEmpty) {
      return Center(
        child: Text('playlist.noTracks'.tr(),
            style: const TextStyle(color: Colors.grey)),
      );
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
          child: Text(
            _playlist!.name,
            style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
          ),
        ),
        Expanded(
          child: ListView.separated(
            padding:
                const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
            itemCount: _playlist!.tracks.length,
            separatorBuilder: (context, _) => const Divider(height: 1),
            itemBuilder: (context, i) {
              final track = _playlist!.tracks[i];
              return _TrackTile(index: i + 1, track: track);
            },
          ),
        ),
      ],
    );
  }
}

class _FilterField extends StatelessWidget {
  final String label;
  final TextEditingController controller;
  final double width;
  final bool isNumber;

  const _FilterField({
    required this.label,
    required this.controller,
    required this.width,
    this.isNumber = false,
  });

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: width,
      child: TextField(
        controller: controller,
        keyboardType: isNumber ? TextInputType.number : TextInputType.text,
        decoration: InputDecoration(
          labelText: label,
          contentPadding:
              const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
        ),
      ),
    );
  }
}

class _TrackTile extends StatelessWidget {
  final int index;
  final EnrichedTrackMetadata track;

  const _TrackTile({required this.index, required this.track});

  @override
  Widget build(BuildContext context) {
    final bpm = track.audioFeatures?.bpm;
    final key = track.audioFeatures?.musicalKey;
    final genres = track.genres.take(2).join(', ');
    final duration = _formatDuration(track.durationMs);

    return ListTile(
      dense: true,
      leading: CircleAvatar(
        backgroundColor: AppColors.primaryViolet.withValues(alpha: 0.2),
        radius: 16,
        child: Text(
          '$index',
          style: const TextStyle(
              fontSize: 11,
              fontWeight: FontWeight.bold,
              color: AppColors.primaryViolet),
        ),
      ),
      title: Text(
        '${track.artist} — ${track.title}',
        style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w500),
        overflow: TextOverflow.ellipsis,
      ),
      subtitle: Text(
        [
          if (track.album.isNotEmpty) track.album,
          if (genres.isNotEmpty) genres,
        ].join(' · '),
        style: const TextStyle(fontSize: 11),
        overflow: TextOverflow.ellipsis,
      ),
      trailing: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (bpm != null) _Chip('${bpm.toStringAsFixed(0)} BPM'),
          if (key != null) ...[
            const SizedBox(width: 4),
            _Chip(key),
          ],
          const SizedBox(width: 4),
          Text(duration, style: const TextStyle(fontSize: 11, color: Colors.grey)),
        ],
      ),
    );
  }

  String _formatDuration(int ms) {
    final s = ms ~/ 1000;
    final m = s ~/ 60;
    final sec = s % 60;
    return '$m:${sec.toString().padLeft(2, '0')}';
  }
}

class _Chip extends StatelessWidget {
  final String label;
  const _Chip(this.label);

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(
        color: AppColors.primaryCyan.withValues(alpha: 0.15),
        borderRadius: BorderRadius.circular(4),
      ),
      child: Text(
        label,
        style: const TextStyle(
            fontSize: 10,
            fontWeight: FontWeight.w600,
            color: AppColors.primaryCyan),
      ),
    );
  }
}
