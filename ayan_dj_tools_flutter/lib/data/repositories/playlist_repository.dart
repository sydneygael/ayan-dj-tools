import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/constants/api_endpoints.dart';
import '../../providers/settings_provider.dart';
import '../models/models.dart';

class PlaylistRepository {
  final Dio _dio;

  PlaylistRepository(String baseUrl)
      : _dio = Dio(BaseOptions(
          baseUrl: baseUrl,
          connectTimeout: const Duration(seconds: 10),
          receiveTimeout: const Duration(seconds: 30),
        ));

  Future<Playlist> generate({
    int bpmMin = 120,
    int bpmMax = 145,
    String genre = '',
  }) async {
    final res = await _dio.post(
      ApiEndpoints.playlistGenerate,
      data: {
        'bpmMin': bpmMin,
        'bpmMax': bpmMax,
        'genre': genre,
      },
    );
    return Playlist.fromJson(res.data as Map<String, dynamic>);
  }
}

final playlistRepositoryProvider = Provider<PlaylistRepository>((ref) {
  final baseUrl = ref.watch(settingsProvider).apiUrl;
  return PlaylistRepository(baseUrl);
});
