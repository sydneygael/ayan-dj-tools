import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/models.dart';
import '../../core/constants/api_endpoints.dart';
import '../../providers/settings_provider.dart';

class PlanRepository {
  final Dio _dio;

  PlanRepository(String baseUrl)
      : _dio = Dio(BaseOptions(
          baseUrl: baseUrl,
          connectTimeout: const Duration(seconds: 30),
          receiveTimeout: const Duration(seconds: 60),
        ));

  Future<TaggingPlan> createPlan({
    required List<String> filePaths,
    required OperatingMode mode,
  }) async {
    final res = await _dio.post(
      ApiEndpoints.planCreate,
      data: {
        'filePaths': filePaths,
        'mode': mode.toApiString(),
      },
    );
    return TaggingPlan.fromJson(res.data as Map<String, dynamic>);
  }

  Future<TaggingPlan> getPlan(String id) async {
    final res = await _dio.get(ApiEndpoints.plan(id));
    return TaggingPlan.fromJson(res.data as Map<String, dynamic>);
  }

  Future<TaggingPlan> approvePlan(String id) async {
    final res = await _dio.put(ApiEndpoints.planApprove(id));
    return TaggingPlan.fromJson(res.data as Map<String, dynamic>);
  }

  Future<BatchApplyResult> executePlan(String id) async {
    final res = await _dio.post(ApiEndpoints.planExecute(id));
    return BatchApplyResult.fromJson(res.data as Map<String, dynamic>);
  }

  Future<TagOperation?> getCurrentOperation(String id) async {
    final res = await _dio.get(ApiEndpoints.planCurrent(id));
    if (res.data == null) return null;
    return TagOperation.fromJson(res.data as Map<String, dynamic>);
  }

  Future<void> confirmOperation(
      String id, int index, bool approve) async {
    await _dio.post(
      ApiEndpoints.planConfirmOperation(id, index),
      data: {'approve': approve},
    );
  }

  Future<void> autoExecute(String id) async {
    await _dio.post(ApiEndpoints.planAutoExecute(id));
  }

  Future<void> deletePlan(String id) async {
    await _dio.delete(ApiEndpoints.plan(id));
  }

  Future<List<TaggingHistoryEntry>> getPlanHistory(String id) async {
    final res = await _dio.get(ApiEndpoints.planHistory(id));
    return (res.data as List)
        .map((e) => TaggingHistoryEntry.fromJson(e as Map<String, dynamic>))
        .toList();
  }
}

final planRepositoryProvider = Provider<PlanRepository>((ref) {
  final baseUrl = ref.watch(settingsProvider).apiUrl;
  return PlanRepository(baseUrl);
});
