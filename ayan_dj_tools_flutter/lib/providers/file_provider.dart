import 'dart:io';
import 'package:file_picker/file_picker.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../core/constants/audio_extensions.dart';

class FileNotifier extends Notifier<List<String>> {
  @override
  List<String> build() => [];

  void addFiles(List<String> paths) {
    final current = Set<String>.from(state);
    for (final p in paths) {
      current.add(p);
    }
    state = current.toList();
  }

  void remove(String path) {
    state = state.where((p) => p != path).toList();
  }

  void clear() {
    state = [];
  }

  /// Opens native file picker for multi-select audio files.
  Future<void> pickFiles() async {
    final result = await FilePicker.platform.pickFiles(
      allowMultiple: true,
      type: FileType.custom,
      allowedExtensions: AudioExtensions.list,
    );
    if (result != null) {
      addFiles(result.paths.whereType<String>().toList());
    }
  }

  /// Opens native folder picker and recursively scans for audio files.
  Future<void> pickFolder() async {
    final result = await FilePicker.platform.getDirectoryPath();
    if (result != null) {
      final files = _scanDirectory(Directory(result));
      addFiles(files);
    }
  }

  List<String> _scanDirectory(Directory dir) {
    final results = <String>[];
    try {
      for (final entity in dir.listSync(recursive: false)) {
        if (entity is Directory) {
          results.addAll(_scanDirectory(entity));
        } else if (entity is File &&
            AudioExtensions.isAudio(entity.path)) {
          results.add(entity.path);
        }
      }
    } catch (_) {}
    return results;
  }
}

final fileProvider = NotifierProvider<FileNotifier, List<String>>(
  FileNotifier.new,
);

/// Currently selected single file for audio preview.
final selectedFileProvider = StateProvider<String?>((ref) => null);
