import 'dart:convert';
import 'dart:io';

import 'package:easy_localization/easy_localization.dart';
import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:path_provider/path_provider.dart';
import '../../../core/utils/notification_service.dart';
import '../../../data/models/models.dart';
import '../../../providers/mode_provider.dart';
import '../../../providers/settings_provider.dart';
import '../../../providers/theme_provider.dart';

class SettingsPage extends ConsumerStatefulWidget {
  const SettingsPage({super.key});

  @override
  ConsumerState<SettingsPage> createState() => _SettingsPageState();
}

class _SettingsPageState extends ConsumerState<SettingsPage> {
  late TextEditingController _apiUrlController;

  @override
  void initState() {
    super.initState();
    _apiUrlController = TextEditingController(
      text: ref.read(settingsProvider).apiUrl,
    );
  }

  @override
  void dispose() {
    _apiUrlController.dispose();
    super.dispose();
  }

  Future<void> _export() async {
    final settings = ref.read(settingsProvider);
    final isDark = ref.read(themeProvider);
    final mode = ref.read(modeProvider);

    final data = jsonEncode({
      'apiUrl': settings.apiUrl,
      'wsEnabled': settings.wsEnabled,
      'darkTheme': isDark,
      'mode': mode.toApiString(),
    });

    try {
      final dir = await getDownloadsDirectory() ??
          await getApplicationDocumentsDirectory();
      final file =
          File('${dir.path}/ayan-dj-settings.json');
      await file.writeAsString(data);
      NotificationService.show('settings.exported'.tr());
    } catch (_) {
      NotificationService.error('errors.network'.tr());
    }
  }

  Future<void> _import() async {
    final result = await FilePicker.platform.pickFiles(
      type: FileType.custom,
      allowedExtensions: ['json'],
    );
    if (result == null || result.files.single.path == null) return;

    try {
      final content =
          await File(result.files.single.path!).readAsString();
      final json = jsonDecode(content) as Map<String, dynamic>;

      final apiUrl = json['apiUrl'] as String?;
      final wsEnabled = json['wsEnabled'] as bool?;
      final darkTheme = json['darkTheme'] as bool?;
      final modeStr = json['mode'] as String?;

      if (apiUrl != null) {
        await ref.read(settingsProvider.notifier).setApiUrl(apiUrl);
        _apiUrlController.text = apiUrl;
      }
      if (wsEnabled != null) {
        await ref.read(settingsProvider.notifier).setWsEnabled(wsEnabled);
      }
      if (darkTheme != null) {
        await ref.read(themeProvider.notifier).setDark(darkTheme);
      }
      if (modeStr != null) {
        await ref
            .read(modeProvider.notifier)
            .setMode(OperatingModeX.fromString(modeStr));
      }

      NotificationService.show('settings.imported'.tr());
    } catch (_) {
      NotificationService.error('settings.invalidFile'.tr());
    }
  }

  @override
  Widget build(BuildContext context) {
    final settings = ref.watch(settingsProvider);
    final isDark = ref.watch(themeProvider);
    final mode = ref.watch(modeProvider);

    return Scaffold(
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('settings.title'.tr(),
                style: const TextStyle(
                    fontSize: 24, fontWeight: FontWeight.bold)),
            const SizedBox(height: 24),

            // Connection
            _SectionCard(
              title: 'settings.connection'.tr(),
              children: [
                ListTile(
                  title: Text('settings.apiUrl'.tr()),
                  subtitle: TextField(
                    controller: _apiUrlController,
                    onSubmitted: (v) => ref
                        .read(settingsProvider.notifier)
                        .setApiUrl(v.trim()),
                    decoration: const InputDecoration(
                      contentPadding:
                          EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                    ),
                  ),
                ),
                SwitchListTile(
                  title: Text('settings.wsEnabled'.tr()),
                  value: settings.wsEnabled,
                  onChanged: (v) =>
                      ref.read(settingsProvider.notifier).setWsEnabled(v),
                ),
              ],
            ),
            const SizedBox(height: 16),

            // Preferences
            _SectionCard(
              title: 'settings.preferences'.tr(),
              children: [
                SwitchListTile(
                  title: Text('settings.darkTheme'.tr()),
                  value: isDark,
                  onChanged: (v) =>
                      ref.read(themeProvider.notifier).setDark(v),
                ),
                ListTile(
                  title: Text('settings.defaultMode'.tr()),
                  trailing: DropdownButton<OperatingMode>(
                    value: mode,
                    items: [
                      DropdownMenuItem(
                        value: OperatingMode.plan,
                        child: Text('mode.plan'.tr()),
                      ),
                      DropdownMenuItem(
                        value: OperatingMode.manual,
                        child: Text('mode.manual'.tr()),
                      ),
                      DropdownMenuItem(
                        value: OperatingMode.apply,
                        child: Text('mode.apply'.tr()),
                      ),
                    ],
                    onChanged: (m) {
                      if (m != null) {
                        ref.read(modeProvider.notifier).setMode(m);
                      }
                    },
                  ),
                ),
                ListTile(
                  title: Text('settings.language'.tr()),
                  trailing: SegmentedButton<Locale>(
                    segments: const [
                      ButtonSegment(
                        value: Locale('fr'),
                        label: Text('FR'),
                      ),
                      ButtonSegment(
                        value: Locale('en'),
                        label: Text('EN'),
                      ),
                    ],
                    selected: {context.locale},
                    onSelectionChanged: (locales) {
                      if (locales.isNotEmpty) {
                        context.setLocale(locales.first);
                      }
                    },
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),

            // Export / Import
            _SectionCard(
              title: 'settings.exportImport'.tr(),
              children: [
                ListTile(
                  title: Text('settings.export'.tr()),
                  subtitle: const Text('ayan-dj-settings.json',
                      style:
                          TextStyle(fontFamily: 'monospace', fontSize: 11)),
                  trailing: OutlinedButton.icon(
                    icon: const Icon(Icons.download, size: 16),
                    label: Text('settings.export'.tr()),
                    onPressed: _export,
                  ),
                ),
                ListTile(
                  title: Text('settings.import'.tr()),
                  subtitle: Text('settings.invalidFile'.tr(),
                      style: const TextStyle(
                          fontStyle: FontStyle.italic, fontSize: 11)),
                  trailing: OutlinedButton.icon(
                    icon: const Icon(Icons.upload, size: 16),
                    label: Text('settings.import'.tr()),
                    onPressed: _import,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _SectionCard extends StatelessWidget {
  final String title;
  final List<Widget> children;

  const _SectionCard({required this.title, required this.children});

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
            child: Text(
              title,
              style: const TextStyle(
                  fontWeight: FontWeight.w600, fontSize: 14),
            ),
          ),
          const Divider(height: 1),
          ...children,
        ],
      ),
    );
  }
}
