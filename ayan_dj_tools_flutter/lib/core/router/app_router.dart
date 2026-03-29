import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../ui/layout/app_shell.dart';
import '../../ui/pages/chat/chat_page.dart';
import '../../ui/pages/plan/plan_review_page.dart';
import '../../ui/pages/history/history_page.dart';
import '../../ui/pages/stats/stats_page.dart';
import '../../ui/pages/settings/settings_page.dart';
import '../../ui/pages/playlist/playlist_page.dart';

final appRouter = GoRouter(
  initialLocation: '/',
  routes: [
    ShellRoute(
      builder: (context, state, child) => AppShell(child: child),
      routes: [
        GoRoute(
          path: '/',
          pageBuilder: (context, state) => _fade(state, const ChatPage()),
        ),
        GoRoute(
          path: '/plan/:id',
          pageBuilder: (context, state) => _fade(
            state,
            PlanReviewPage(planId: state.pathParameters['id']!),
          ),
        ),
        GoRoute(
          path: '/history',
          pageBuilder: (context, state) => _fade(
            state,
            HistoryPage(initialPlanId: state.uri.queryParameters['planId']),
          ),
        ),
        GoRoute(
          path: '/stats',
          pageBuilder: (context, state) => _fade(state, const StatsPage()),
        ),
        GoRoute(
          path: '/settings',
          pageBuilder: (context, state) => _fade(state, const SettingsPage()),
        ),
        GoRoute(
          path: '/playlist',
          pageBuilder: (context, state) => _fade(state, const PlaylistPage()),
        ),
      ],
    ),
  ],
);

CustomTransitionPage<void> _fade(GoRouterState state, Widget child) =>
    CustomTransitionPage<void>(
      key: state.pageKey,
      child: child,
      transitionDuration: const Duration(milliseconds: 200),
      transitionsBuilder: (context, animation, secondary, widget) =>
          FadeTransition(opacity: animation, child: widget),
    );
