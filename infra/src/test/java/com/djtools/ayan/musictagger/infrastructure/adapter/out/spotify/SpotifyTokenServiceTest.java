package com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify;

import com.djtools.ayan.musictagger.infrastructure.adapter.out.spotify.exception.SpotifyAuthException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpotifyTokenServiceTest {

    private MockWebServer mockServer;
    private SpotifyTokenService tokenService;

    @BeforeEach
    void setUp() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();
        tokenService = new SpotifyTokenService(
                mockServer.url("/api/token").toString(),
                "test-client-id",
                "test-client-secret"
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        mockServer.shutdown();
    }

    @Test
    void shouldFetchAccessToken() {
        mockServer.enqueue(new MockResponse()
                .setBody("""
                        {"access_token":"test-token","token_type":"Bearer","expires_in":3600}
                        """)
                .addHeader("Content-Type", "application/json"));

        String token = tokenService.getAccessToken();

        assertThat(token).isEqualTo("test-token");
    }

    @Test
    void shouldReuseTokenWithinExpiry() {
        mockServer.enqueue(new MockResponse()
                .setBody("""
                        {"access_token":"test-token","token_type":"Bearer","expires_in":3600}
                        """)
                .addHeader("Content-Type", "application/json"));

        String token1 = tokenService.getAccessToken();
        String token2 = tokenService.getAccessToken();

        assertThat(token1).isEqualTo(token2);
        assertThat(mockServer.getRequestCount()).isEqualTo(1);
    }

    @Test
    void shouldRefreshExpiredToken() {
        // First token with very short expiry (will be considered expired due to 60s buffer)
        mockServer.enqueue(new MockResponse()
                .setBody("""
                        {"access_token":"token-1","token_type":"Bearer","expires_in":30}
                        """)
                .addHeader("Content-Type", "application/json"));
        mockServer.enqueue(new MockResponse()
                .setBody("""
                        {"access_token":"token-2","token_type":"Bearer","expires_in":3600}
                        """)
                .addHeader("Content-Type", "application/json"));

        String token1 = tokenService.getAccessToken();
        String token2 = tokenService.getAccessToken();

        assertThat(token1).isEqualTo("token-1");
        assertThat(token2).isEqualTo("token-2");
        assertThat(mockServer.getRequestCount()).isEqualTo(2);
    }

    @Test
    void shouldThrowOnAuthFailure() {
        mockServer.enqueue(new MockResponse().setResponseCode(401));

        assertThatThrownBy(() -> tokenService.getAccessToken())
                .isInstanceOf(SpotifyAuthException.class)
                .hasMessageContaining("Failed to obtain Spotify access token");
    }
}
