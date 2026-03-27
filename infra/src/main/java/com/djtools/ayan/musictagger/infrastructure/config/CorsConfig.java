package com.djtools.ayan.musictagger.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * Configuration CORS globale de l'application.
 *
 * <p>Utilise un {@link CorsFilter} (filtre servlet) plutôt que {@code WebMvcConfigurer}
 * afin de couvrir tous les endpoints, y compris ceux de Spring Boot Actuator
 * qui ne passent pas par le DispatcherServlet principal.</p>
 *
 * <p>Seule l'origine du frontend React ({@code localhost:5173}) est autorisée.
 * Les credentials sont activés pour permettre l'envoi de cookies et d'en-têtes
 * d'authentification.</p>
 */
@Configuration
public class CorsConfig {

    /**
     * Déclare le filtre CORS appliqué à toutes les routes ({@code /**}).
     *
     * <p>Le filtre intercepte les requêtes HTTP avant tout routing Spring,
     * ce qui garantit que les preflight OPTIONS et les requêtes cross-origin
     * vers {@code /actuator/**} reçoivent bien les en-têtes CORS attendus.</p>
     */
    @Bean
    public CorsFilter corsFilter() {
        final var config = new CorsConfiguration();
        // Frontend React (Vite dev server)
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.addAllowedHeader("*");
        // Requis pour les appels authentifiés (ex : Authorization header, cookies)
        config.setAllowCredentials(true);

        final var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
