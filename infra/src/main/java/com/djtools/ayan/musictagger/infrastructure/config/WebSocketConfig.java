package com.djtools.ayan.musictagger.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuration WebSocket STOMP de l'application.
 *
 * <p>Active le broker de messages in-memory et expose un endpoint SockJS
 * utilisé par le frontend React pour la communication temps réel
 * (réponses de l'agent IA, progression des plans de tagging).</p>
 *
 * <p>L'origine autorisée est restreinte au frontend React ({@code localhost:5173}),
 * alignée avec la politique CORS définie dans {@link CorsConfig}.</p>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configure le broker de messages STOMP.
     *
     * <ul>
     *   <li>{@code /topic} — broker simple in-memory pour les messages broadcast
     *       (ex : {@code /topic/responses}, {@code /topic/plan/{id}/progress})</li>
     *   <li>{@code /app} — préfixe des destinations gérées par les {@code @MessageMapping}
     *       des controllers (ex : {@code /app/chat})</li>
     * </ul>
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Enregistre l'endpoint WebSocket STOMP.
     *
     * <p>SockJS est activé comme transport de fallback pour les environnements
     * ne supportant pas WebSocket natif (proxies, anciens navigateurs).</p>
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins("http://localhost:5173").withSockJS();
    }
}
