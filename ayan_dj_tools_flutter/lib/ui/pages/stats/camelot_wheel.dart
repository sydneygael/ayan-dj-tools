import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';

/// Camelot Wheel — CustomPainter port of CamelotWheel.tsx.
/// Draws two concentric rings (major outer, minor inner) with 12 segments each.
/// Segment opacity scales with the track count for that key.
class CamelotWheel extends StatelessWidget {
  final Map<String, int> keyDistribution;

  const CamelotWheel({super.key, required this.keyDistribution});

  @override
  Widget build(BuildContext context) {
    final counts = _buildCamelotCounts(keyDistribution);
    if (counts.isEmpty) {
      return const Center(
        child: Text('No key data', style: TextStyle(color: Colors.grey)),
      );
    }
    return AspectRatio(
      aspectRatio: 1,
      child: CustomPaint(
        painter: _CamelotWheelPainter(counts: counts),
      ),
    );
  }

  static Map<String, int> _buildCamelotCounts(Map<String, int> keyDist) {
    final camelotMap = {
      'B': ('1B', 0),
      'F#': ('2B', 1),
      'Db': ('3B', 2),
      'Ab': ('4B', 3),
      'Eb': ('5B', 4),
      'Bb': ('6B', 5),
      'F': ('7B', 6),
      'C': ('8B', 7),
      'G': ('9B', 8),
      'D': ('10B', 9),
      'A': ('11B', 10),
      'E': ('12B', 11),
      'G#m': ('1A', 0),
      'Abm': ('1A', 0),
      'D#m': ('2A', 1),
      'Ebm': ('2A', 1),
      'Bbm': ('3A', 2),
      'Fm': ('4A', 3),
      'Cm': ('5A', 4),
      'Gm': ('6A', 5),
      'Dm': ('7A', 6),
      'Am': ('8A', 7),
      'Em': ('9A', 8),
      'Bm': ('10A', 9),
      'F#m': ('11A', 10),
      'C#m': ('12A', 11),
      'Dbm': ('12A', 11),
    };

    final counts = <String, int>{};
    for (final entry in keyDist.entries) {
      final mapped = camelotMap[entry.key];
      if (mapped == null) continue;
      counts[mapped.$1] = (counts[mapped.$1] ?? 0) + entry.value;
    }
    return counts;
  }
}

class _CamelotWheelPainter extends CustomPainter {
  final Map<String, int> counts;

  _CamelotWheelPainter({required this.counts});

  static const int _segments = 12;
  static const double _segAngle = 360 / _segments;

  @override
  void paint(Canvas canvas, Size size) {
    final cx = size.width / 2;
    final cy = size.height / 2;
    final scale = size.width / 300;

    final rMajorInner = 80 * scale;
    final rMajorOuter = 140 * scale;
    final rMinorInner = 30 * scale;
    final rMinorOuter = 75 * scale;

    final maxCount = counts.values.fold(0, math.max).clamp(1, double.infinity);

    final fillPaint = Paint()..style = PaintingStyle.fill;
    final strokePaint = Paint()
      ..style = PaintingStyle.stroke
      ..color = Colors.grey.shade800
      ..strokeWidth = 0.5 * scale;

    // Draw rings
    for (final ring in [
      _Ring('B', rMajorInner, rMajorOuter, 10 * scale),
      _Ring('A', rMinorInner, rMinorOuter, 8 * scale),
    ]) {
      for (int i = 0; i < _segments; i++) {
        final code = '${i + 1}${ring.suffix}';
        final count = counts[code] ?? 0;
        final opacity = count > 0
            ? 0.2 + 0.8 * (count / maxCount)
            : 0.05;

        final startDeg = i * _segAngle;
        final endDeg = startDeg + _segAngle;

        final path = _arcPath(
            cx, cy, ring.rInner, ring.rOuter, startDeg, endDeg);

        fillPaint.color =
            AppColors.primaryCyan.withValues(alpha: opacity);
        canvas.drawPath(path, fillPaint);
        canvas.drawPath(path, strokePaint);

        // Label
        final midDeg = startDeg + _segAngle / 2;
        final labelR = (ring.rInner + ring.rOuter) / 2;
        final lp = _polar(cx, cy, labelR, midDeg);

        final tp = TextPainter(
          text: TextSpan(
            text: code,
            style: TextStyle(
              color: Colors.white.withValues(alpha: 0.85),
              fontSize: ring.fontSize,
              fontWeight: count > 0 ? FontWeight.w600 : FontWeight.w400,
            ),
          ),
          textDirection: TextDirection.ltr,
        )..layout();
        tp.paint(canvas, Offset(lp.dx - tp.width / 2, lp.dy - tp.height / 2));
      }
    }
  }

  Path _arcPath(double cx, double cy, double rInner, double rOuter,
      double startDeg, double endDeg) {
    final s1 = _polar(cx, cy, rOuter, startDeg);
    final e1 = _polar(cx, cy, rOuter, endDeg);
    final s2 = _polar(cx, cy, rInner, endDeg);
    final e2 = _polar(cx, cy, rInner, startDeg);

    final largeArc = (endDeg - startDeg).abs() > 180;

    final path = Path()
      ..moveTo(s1.dx, s1.dy)
      ..arcToPoint(e1,
          radius: Radius.circular(rOuter), clockwise: true, largeArc: largeArc)
      ..lineTo(s2.dx, s2.dy)
      ..arcToPoint(e2,
          radius: Radius.circular(rInner), clockwise: false, largeArc: largeArc)
      ..close();
    return path;
  }

  Offset _polar(double cx, double cy, double r, double angleDeg) {
    final rad = (angleDeg - 90) * math.pi / 180;
    return Offset(cx + r * math.cos(rad), cy + r * math.sin(rad));
  }

  @override
  bool shouldRepaint(_CamelotWheelPainter old) => old.counts != counts;
}

class _Ring {
  final String suffix;
  final double rInner;
  final double rOuter;
  final double fontSize;

  const _Ring(this.suffix, this.rInner, this.rOuter, this.fontSize);
}
