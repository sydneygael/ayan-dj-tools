class AudioExtensions {
  AudioExtensions._();

  static const List<String> list = ['mp3', 'flac', 'wav', 'aiff', 'm4a', 'ogg'];

  static const Set<String> set = {'.mp3', '.flac', '.wav', '.aiff', '.m4a', '.ogg'};

  static bool isAudio(String path) {
    final ext = path.toLowerCase().split('.').last;
    return list.contains(ext);
  }
}
