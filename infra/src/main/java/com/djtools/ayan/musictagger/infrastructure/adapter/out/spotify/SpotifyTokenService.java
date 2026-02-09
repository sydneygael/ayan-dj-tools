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
            return cachedToken;
        }
        return refreshToken();
    }

    private String refreshToken() {
        log.debug("Refreshing Spotify access token");
        String credentials = clientId + ":" + clientSecret;
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        try {
            SpotifyTokenResponse response = tokenClient.post()
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
            log.debug("Spotify token refreshed, expires in {} seconds", response.expires_in());
            return cachedToken;
        } catch (SpotifyAuthException e) {
            throw e;
        } catch (Exception e) {
            throw new SpotifyAuthException("Failed to obtain Spotify access token", e);
        }
    }
}
