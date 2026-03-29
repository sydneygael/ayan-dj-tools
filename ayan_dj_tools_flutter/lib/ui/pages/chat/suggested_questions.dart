import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';

class SuggestedQuestions extends StatelessWidget {
  final void Function(String question) onSelect;

  const SuggestedQuestions({super.key, required this.onSelect});

  @override
  Widget build(BuildContext context) {
    final suggestions = List.generate(
      6,
      (i) => 'chat.suggestions_$i'.tr(),
    );

    return Padding(
      padding: const EdgeInsets.only(top: 24),
      child: Wrap(
        spacing: 8,
        runSpacing: 8,
        alignment: WrapAlignment.center,
        children: suggestions
            .map((q) => ActionChip(
                  label: Text(q, style: const TextStyle(fontSize: 12)),
                  side: const BorderSide(color: AppColors.primaryCyan, width: 0.5),
                  backgroundColor: AppColors.primaryCyan.withValues(alpha: 0.05),
                  onPressed: () => onSelect(q),
                ))
            .toList(),
      ),
    );
  }
}
