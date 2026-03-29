import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../data/models/models.dart';

const _keyMode = 'mode.default';

class ModeNotifier extends Notifier<OperatingMode> {
  @override
  OperatingMode build() {
    _load();
    return OperatingMode.plan;
  }

  Future<void> _load() async {
    final prefs = await SharedPreferences.getInstance();
    final saved = prefs.getString(_keyMode);
    if (saved != null) {
      state = OperatingMode.values.firstWhere(
        (m) => m.name == saved,
        orElse: () => OperatingMode.plan,
      );
    }
  }

  Future<void> setMode(OperatingMode mode) async {
    state = mode;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_keyMode, mode.name);
  }
}

final modeProvider = NotifierProvider<ModeNotifier, OperatingMode>(
  ModeNotifier.new,
);
