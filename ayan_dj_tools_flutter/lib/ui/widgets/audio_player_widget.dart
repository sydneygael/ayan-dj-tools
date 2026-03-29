import 'dart:async';

import 'package:audioplayers/audioplayers.dart';
import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';

/// Compact audio player in the sidebar. Shows filename, play/pause, seek, volume.
/// Mirrors AudioPlayer.tsx — uses file:// local source via audioplayers.
class AudioPlayerWidget extends StatefulWidget {
  final String filepath;

  const AudioPlayerWidget({super.key, required this.filepath});

  @override
  State<AudioPlayerWidget> createState() => _AudioPlayerWidgetState();
}

class _AudioPlayerWidgetState extends State<AudioPlayerWidget> {
  final AudioPlayer _player = AudioPlayer();

  bool _playing = false;
  Duration _position = Duration.zero;
  Duration _duration = Duration.zero;
  double _volume = 0.8;

  final List<StreamSubscription> _subs = [];

  @override
  void initState() {
    super.initState();
    _subs.add(_player.onPlayerStateChanged.listen((s) {
      if (mounted) setState(() => _playing = s == PlayerState.playing);
    }));
    _subs.add(_player.onPositionChanged.listen((p) {
      if (mounted) setState(() => _position = p);
    }));
    _subs.add(_player.onDurationChanged.listen((d) {
      if (mounted) setState(() => _duration = d);
    }));
    _subs.add(_player.onPlayerComplete.listen((_) {
      if (mounted) setState(() => _playing = false);
    }));
    _player.setVolume(_volume);
    _player.setSourceDeviceFile(widget.filepath);
  }

  @override
  void didUpdateWidget(AudioPlayerWidget old) {
    super.didUpdateWidget(old);
    if (old.filepath != widget.filepath) {
      _player.stop();
      _player.setSourceDeviceFile(widget.filepath);
      setState(() {
        _playing = false;
        _position = Duration.zero;
        _duration = Duration.zero;
      });
    }
  }

  @override
  void dispose() {
    _player.dispose();
    for (final s in _subs) {
      s.cancel();
    }
    super.dispose();
  }

  void _togglePlay() {
    if (_playing) {
      _player.pause();
    } else {
      _player.resume();
    }
  }

  void _seek(double seconds) {
    _player.seek(Duration(milliseconds: (seconds * 1000).round()));
  }

  String _fmt(Duration d) {
    final m = d.inMinutes.remainder(60).toString().padLeft(2, '0');
    final s = d.inSeconds.remainder(60).toString().padLeft(2, '0');
    return '$m:$s';
  }

  @override
  Widget build(BuildContext context) {
    final filename = widget.filepath.split(RegExp(r'[/\\]')).last;
    final maxSeconds = _duration.inMilliseconds / 1000.0;
    final currentSeconds = _position.inMilliseconds / 1000.0;

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(
            filename,
            style: const TextStyle(fontSize: 11),
            overflow: TextOverflow.ellipsis,
          ),
          Row(
            children: [
              IconButton(
                iconSize: 20,
                padding: EdgeInsets.zero,
                constraints: const BoxConstraints(minWidth: 28, minHeight: 28),
                icon: Icon(
                  _playing ? Icons.pause : Icons.play_arrow,
                  color: AppColors.primaryCyan,
                ),
                tooltip: _playing ? 'audio.pause'.tr() : 'audio.play'.tr(),
                onPressed: _togglePlay,
              ),
              Text(
                _fmt(_position),
                style: const TextStyle(fontSize: 10),
              ),
              Expanded(
                child: Slider(
                  value: currentSeconds.clamp(0, maxSeconds > 0 ? maxSeconds : 1),
                  max: maxSeconds > 0 ? maxSeconds : 1,
                  onChanged: maxSeconds > 0 ? _seek : null,
                  activeColor: AppColors.primaryCyan,
                ),
              ),
              Text(
                _fmt(_duration),
                style: const TextStyle(fontSize: 10),
              ),
            ],
          ),
          Row(
            children: [
              const Icon(Icons.volume_up, size: 14, color: Colors.grey),
              Expanded(
                child: Slider(
                  value: _volume,
                  onChanged: (v) {
                    setState(() => _volume = v);
                    _player.setVolume(v);
                  },
                  activeColor: AppColors.primaryCyan,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
