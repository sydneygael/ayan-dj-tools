import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';

class WsStatusChip extends StatelessWidget {
  final bool connected;

  const WsStatusChip({super.key, required this.connected});

  @override
  Widget build(BuildContext context) {
    return Tooltip(
      message: connected ? 'ws.connected'.tr() : 'ws.disconnected'.tr(),
      child: Icon(
        connected ? Icons.wifi : Icons.wifi_off,
        size: 16,
        color: connected ? AppColors.success : Colors.grey,
      ),
    );
  }
}
