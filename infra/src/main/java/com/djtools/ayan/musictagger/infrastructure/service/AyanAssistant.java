package com.djtools.ayan.musictagger.infrastructure.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 * Interface déclarative LangChain4j pour l'agent Ayan.
 * chatSync → ChatLanguageModel (blocant, POST /api/agent/chat)
 * chatStream → StreamingChatLanguageModel (SSE, POST /api/agent/chat/stream)
 */
public interface AyanAssistant {
    String chatSync(@MemoryId String conversationId, @UserMessage String message);
    TokenStream chatStream(@MemoryId String conversationId, @UserMessage String message);
}
