import 'package:flutter/material.dart';

class AppColors {
  AppColors._();

  // Primary palette — cyan / violet (mirrors the React MUI theme)
  static const Color primaryCyan = Color(0xFF00BCD4);
  static const Color primaryViolet = Color(0xFF7C4DFF);

  // Dark theme surfaces
  static const Color darkBackground = Color(0xFF121212);
  static const Color darkSurface = Color(0xFF1E1E1E);
  static const Color darkCard = Color(0xFF252525);
  static const Color darkBorder = Color(0xFF333333);

  // Light theme surfaces
  static const Color lightBackground = Color(0xFFF5F5F5);
  static const Color lightSurface = Color(0xFFFFFFFF);
  static const Color lightCard = Color(0xFFFAFAFA);

  // Status colors
  static const Color success = Color(0xFF4CAF50);
  static const Color warning = Color(0xFFFF9800);
  static const Color error = Color(0xFFF44336);
  static const Color info = Color(0xFF2196F3);

  // Tag diff colors
  static const Color tagRemoved = Color(0xFFEF9A9A);   // red-200
  static const Color tagAdded = Color(0xFFA5D6A7);     // green-200

  // Sidebar
  static const double sidebarWidth = 280.0;
}
