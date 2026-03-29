import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'core/router/app_router.dart';
import 'core/theme/app_theme.dart';
import 'core/utils/notification_service.dart';
import 'providers/theme_provider.dart';

class AyanDjToolsApp extends ConsumerWidget {
  const AyanDjToolsApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final isDark = ref.watch(themeProvider);

    return MaterialApp.router(
      title: 'Ayan DJ Tools',
      debugShowCheckedModeBanner: false,
      scaffoldMessengerKey: NotificationService.scaffoldMessengerKey,

      // Theme
      theme: AppTheme.light(),
      darkTheme: AppTheme.dark(),
      themeMode: isDark ? ThemeMode.dark : ThemeMode.light,

      // i18n
      localizationsDelegates: context.localizationDelegates,
      supportedLocales: context.supportedLocales,
      locale: context.locale,

      // Navigation
      routerConfig: appRouter,
    );
  }
}
