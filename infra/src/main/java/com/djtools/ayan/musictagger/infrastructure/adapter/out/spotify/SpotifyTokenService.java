package com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify;

import com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.dto.SpotifyTokenResponse;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.exception.SpotifyAuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

public class SpotifyTokenService {

    private static final Logger log = LoggerFactory.getLogger(SpotifyTokenService.class);
    private static final int EXPIRY_BUFFER_SECONDS = 60;

    private final RestClient tokenClient;
    private final String clientId;
    private final String clientSecret;

    private String cachedToken;
    private Instant tokenExpiry = Instant.MIN;

    public SpotifyTokenService(String authUrl, String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenClient = RestClient.builder()
                .baseUrl(authUrl)
                .build();
    }

    public synchronized String getAccessToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiry)) {
            log.debug("Spotify token still valid (expires in ~{} s)",
                    java.time.Duration.between(Instant.now(), tokenExpiry).getSeconds());
            return cachedToken;
        }
        return refreshToken();
    }

    private String refreshToken() {
        log.info("Refreshing Spotify access token (client_id={})", clientId);
        final var credentials = clientId + ":" + clientSecret;
        final var encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        try {
            final var response = tokenClient.post()
                    .header("Authorization", "Basic " + encoded)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body("grant_type=client_credentials")
                    .retrieve()
                    .body(SpotifyTokenResponse.class);

            if (response == null || response.access_token() == null) {
                throw new SpotifyAuthException("Empty token response from Spotify");
            }

            cachedToken = response.access_token();
            tokenExpiry = Instant.now().plusSeconds(response.expires_in() - EXPIRY_BUFFER_SECONDS);
            log.info("Spotify token refreshed — expires in {} s (effective buffer {}s)",
                    response.expires_in(), EXPIRY_BUFFER_SECONDS);
            return cachedToken;
        } catch (SpotifyAuthException e) {
            log.error("Spotify auth failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Spotify token request failed: {}", e.getMessage(), e);
            throw new SpotifyAuthException("Failed to obtain Spotify access token", e);
        }
    }
}
