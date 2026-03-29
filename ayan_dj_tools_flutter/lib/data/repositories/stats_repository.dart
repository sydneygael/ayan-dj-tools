import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/constants/api_endpoints.dart';
import '../../providers/settings_provider.dart';
import '../models/models.dart';

class StatsRepository {
  final Dio _dio;

  StatsRepository(String baseUrl)
      : _dio = Dio(BaseOptions(
          baseUrl: baseUrl,
          connectTimeout: const Duration(seconds: 10),
          receiveTimeout: const Duration(seconds: 30),
        ));

  Future<StatsReport> getStats() async {
    final res = await _dio.get(ApiEndpoints.stats);
    return StatsReport.fromJson(res.data as Map<String, dynamic>);
  }

  Future<CollectionProfile> getCollection() async {
    final res = await _dio.get(ApiEndpoints.statsCollection);
    return CollectionProfile.fromJson(res.data as Map<String, dynamic>);
  }

  Future<EnrichmentStats> getEnrichment() async {
    final res = await _dio.get(ApiEndpoints.statsEnrichment);
    return EnrichmentStats.fromJson(res.data as Map<String, dynamic>);
  }

  Future<ActivityTimeline> getActivity({String? period}) async {
    final res = await _dio.get(
      ApiEndpoints.statsActivity,
      queryParameters: period != null ? {'period': period} : null,
    );
    return ActivityTimeline.fromJson(res.data as Map<String, dynamic>);
  }
}

final statsRepositoryProvider = Provider<StatsRepository>((ref) {
  final baseUrl = ref.watch(settingsProvider).apiUrl;
  return StatsRepository(baseUrl);
});
