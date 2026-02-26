package com.djtools.ayan.musictagger.infrastructure.service;

import java.time.LocalDateTime;

public record ChatMessage(
        String role,
        String content,
        LocalDateTime timestamp
) {}
