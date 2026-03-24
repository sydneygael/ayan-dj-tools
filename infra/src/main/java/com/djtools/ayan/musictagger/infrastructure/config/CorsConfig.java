package com.djtools.ayan.musictagger.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("http://localhost:4200", "http://localhost:5173", "file://*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false);
        registry.addMapping("/ws/**")
                .allowedOriginPatterns("http://localhost:4200", "http://localhost:5173", "file://*")
                .allowedMethods("GET", "POST")
                .allowedHeaders("*")
                .allowCredentials(false);
    }
}
