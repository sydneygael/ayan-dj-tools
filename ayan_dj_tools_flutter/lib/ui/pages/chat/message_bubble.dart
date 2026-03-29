import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import '../../../core/theme/app_colors.dart';
import '../../../data/models/models.dart';

class MessageBubble extends StatelessWidget {
  final ChatMessage message;
  final bool isStreaming;

  const MessageBubble({
    super.key,
    required this.message,
    this.isStreaming = false,
  });

  @override
  Widget build(BuildContext context) {
    final isUser = message.role == 'user';
    final colorScheme = Theme.of(context).colorScheme;

    return Align(
      alignment: isUser ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        margin: const EdgeInsets.symmetric(vertical: 4, horizontal: 8),
        constraints: BoxConstraints(
          maxWidth: MediaQuery.of(context).size.width * 0.75,
        ),
        decoration: BoxDecoration(
          color: isUser
              ? AppColors.primaryCyan.withValues(alpha: 0.15)
              : colorScheme.surface,
          border: Border.all(
            color: isUser
                ? AppColors.primaryCyan.withValues(alpha: 0.3)
                : AppColors.darkBorder,
          ),
          borderRadius: BorderRadius.only(
            topLeft: const Radius.circular(12),
            topRight: const Radius.circular(12),
            bottomLeft: Radius.circular(isUser ? 12 : 2),
            bottomRight: Radius.circular(isUser ? 2 : 12),
          ),
        ),
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Role label
            Text(
              isUser ? 'Vous' : 'Ayan',
              style: TextStyle(
                fontSize: 10,
                fontWeight: FontWeight.w600,
                color: isUser ? AppColors.primaryCyan : Colors.grey,
              ),
            ),
            const SizedBox(height: 4),
            // Content — markdown for agent, plain text for user
            isUser
                ? Text(message.content, style: const TextStyle(fontSize: 14))
                : MarkdownBody(
                    data: message.content,
                    styleSheet: MarkdownStyleSheet(
                      p: const TextStyle(fontSize: 14),
                      code: TextStyle(
                        fontSize: 13,
                        fontFamily: 'monospace',
                        backgroundColor: Colors.grey.shade800,
                      ),
                    ),
                  ),
            if (isStreaming)
              const Padding(
                padding: EdgeInsets.only(top: 2),
                child: Text('▍',
                    style: TextStyle(
                        color: AppColors.primaryCyan, fontSize: 14)),
              ),
          ],
        ),
      ),
    );
  }
}
