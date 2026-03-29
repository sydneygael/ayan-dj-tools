import 'package:flutter/material.dart';

/// Global snackbar service — use the [scaffoldMessengerKey] in MaterialApp.
class NotificationService {
  NotificationService._();

  static final GlobalKey<ScaffoldMessengerState> scaffoldMessengerKey =
      GlobalKey<ScaffoldMessengerState>();

  static void show(
    String message, {
    bool isError = false,
    Duration duration = const Duration(seconds: 3),
  }) {
    scaffoldMessengerKey.currentState
      ?..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(
          content: Text(message),
          backgroundColor: isError ? Colors.red.shade700 : null,
          behavior: SnackBarBehavior.floating,
          duration: duration,
        ),
      );
  }

  static void error(String message) => show(message, isError: true);
}
