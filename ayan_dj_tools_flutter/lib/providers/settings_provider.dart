import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../core/constants/api_endpoints.dart';

const _keyApiUrl = 'settings.apiUrl';
const _keyWsEnabled = 'settings.wsEnabled';

class SettingsState {
  final String apiUrl;
  final bool wsEnabled;

  const SettingsState({
    this.apiUrl = ApiEndpoints.defaultBaseUrl,
    this.wsEnabled = true,
  });

  /// Derives WebSocket URL from apiUrl (http → http, same host/port).
  String get wsUrl => apiUrl.endsWith('/ws') ? apiUrl : '$apiUrl/ws';

  SettingsState copyWith({String? apiUrl, bool? wsEnabled}) => SettingsState(
        apiUrl: apiUrl ?? this.apiUrl,
        wsEnabled: wsEnabled ?? this.wsEnabled,
      );
}

class SettingsNotifier extends Notifier<SettingsState> {
  @override
  SettingsState build() {
    _load();
    return const SettingsState();
  }

  Future<void> _load() async {
    final prefs = await SharedPreferences.getInstance();
    state = SettingsState(
      apiUrl: prefs.getString(_keyApiUrl) ?? ApiEndpoints.defaultBaseUrl,
      wsEnabled: prefs.getBool(_keyWsEnabled) ?? true,
    );
  }

  Future<void> setApiUrl(String url) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_keyApiUrl, url);
    state = state.copyWith(apiUrl: url);
  }

  Future<void> setWsEnabled(bool enabled) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_keyWsEnabled, enabled);
    state = state.copyWith(wsEnabled: enabled);
  }
}

final settingsProvider = NotifierProvider<SettingsNotifier, SettingsState>(
  SettingsNotifier.new,
);
