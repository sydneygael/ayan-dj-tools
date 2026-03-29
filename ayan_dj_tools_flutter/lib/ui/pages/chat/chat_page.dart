import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_colors.dart';
import '../../../data/models/models.dart';
import '../../../providers/chat_provider.dart';
import '../../../providers/settings_provider.dart';
import '../../widgets/ws_status_chip.dart';
import 'message_bubble.dart';
import 'suggested_questions.dart';

class ChatPage extends ConsumerStatefulWidget {
  const ChatPage({super.key});

  @override
  ConsumerState<ChatPage> createState() => _ChatPageState();
}

class _ChatPageState extends ConsumerState<ChatPage> {
  final _inputController = TextEditingController();
  final _scrollController = ScrollController();
  final _inputFocusNode = FocusNode();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(chatProvider.notifier).connectWs();
    });
  }

  @override
  void dispose() {
    ref.read(chatProvider.notifier).disconnectWs();
    _inputController.dispose();
    _scrollController.dispose();
    _inputFocusNode.dispose();
    super.dispose();
  }

  void _scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scrollController.hasClients) {
        _scrollController.animateTo(
          _scrollController.position.maxScrollExtent,
          duration: const Duration(milliseconds: 200),
          curve: Curves.easeOut,
        );
      }
    });
  }

  Future<void> _send() async {
    final text = _inputController.text.trim();
    if (text.isEmpty) return;
    _inputController.clear();
    await ref.read(chatProvider.notifier).sendMessage(text);
    _scrollToBottom();
  }

  @override
  Widget build(BuildContext context) {
    final chat = ref.watch(chatProvider);
    final wsEnabled = ref.watch(settingsProvider).wsEnabled;

    // Auto-scroll when messages change
    ref.listen(chatProvider, (_, next) {
      if (next.messages.length != (ref.read(chatProvider).messages.length)) {
        _scrollToBottom();
      }
    });

    return Column(
      children: [
        // Message list
        Expanded(
          child: ListView(
            controller: _scrollController,
            padding: const EdgeInsets.symmetric(vertical: 8),
            children: [
              // Empty state — greeting + suggestions
              if (chat.messages.isEmpty && chat.streamingContent == null)
                _buildGreeting(),

              // Messages
              ...chat.messages.map((msg) => MessageBubble(message: msg)),

              // Streaming bubble
              if (chat.streamingContent != null)
                MessageBubble(
                  key: const ValueKey('streaming'),
                  message: _streamingMessage(chat.streamingContent!),
                  isStreaming: true,
                ),

              // Loading indicator (REST fallback)
              if (chat.loading && chat.streamingContent == null)
                const Padding(
                  padding: EdgeInsets.symmetric(vertical: 16),
                  child: Center(child: CircularProgressIndicator()),
                ),

              const SizedBox(height: 8),
            ],
          ),
        ),

        // Input row
        Container(
          decoration: BoxDecoration(
            border: Border(
              top: BorderSide(color: Colors.grey.shade800),
            ),
          ),
          padding: const EdgeInsets.all(8),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              if (wsEnabled)
                Padding(
                  padding: const EdgeInsets.only(right: 8, bottom: 8),
                  child: WsStatusChip(connected: chat.wsConnected),
                ),

              Expanded(
                child: KeyboardListener(
                  focusNode: FocusNode(),
                  onKeyEvent: (event) {
                    if (event is KeyDownEvent &&
                        event.logicalKey == LogicalKeyboardKey.enter &&
                        !HardwareKeyboard.instance.isShiftPressed) {
                      _send();
                    }
                  },
                  child: TextField(
                    controller: _inputController,
                    focusNode: _inputFocusNode,
                    decoration: InputDecoration(
                      hintText: 'chat.placeholder'.tr(),
                      contentPadding: const EdgeInsets.symmetric(
                          horizontal: 12, vertical: 8),
                    ),
                    maxLines: 3,
                    minLines: 1,
                    textInputAction: TextInputAction.newline,
                  ),
                ),
              ),

              const SizedBox(width: 8),

              // Send / Stop button
              if (chat.loading || chat.streamingContent != null)
                IconButton(
                  icon: const Icon(Icons.stop_circle),
                  color: Colors.red,
                  tooltip: 'Arreter',
                  onPressed: ref.read(chatProvider.notifier).stopStream,
                )
              else
                IconButton(
                  icon: const Icon(Icons.send),
                  color: AppColors.primaryCyan,
                  tooltip: 'chat.sendLabel'.tr(),
                  onPressed: () => _send(),
                ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildGreeting() {
    return Padding(
      padding: const EdgeInsets.only(top: 48, bottom: 24),
      child: Column(
        children: [
          const Icon(Icons.smart_toy, size: 64, color: AppColors.primaryCyan),
          const SizedBox(height: 16),
          Text(
            'chat.greeting'.tr(),
            style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 8),
          Text(
            'chat.subtitle'.tr(),
            style: TextStyle(color: Colors.grey.shade500),
          ),
          SuggestedQuestions(
            onSelect: (q) {
              _inputController.text = q;
              _inputFocusNode.requestFocus();
            },
          ),
        ],
      ),
    );
  }

  ChatMessage _streamingMessage(String content) => ChatMessage(
        role: 'agent',
        content: content,
        timestamp: DateTime.now().toIso8601String(),
      );
}
