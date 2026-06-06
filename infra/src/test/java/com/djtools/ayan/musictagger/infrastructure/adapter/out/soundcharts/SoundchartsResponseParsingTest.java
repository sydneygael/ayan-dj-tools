package com.djtools.ayan.musictagger.infrastructure.adapter.out.soundcharts;

import com.djtools.ayan.musictagger.domain.model.EnrichmentResult;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.soundcharts.dto.SoundchartsLyricsResponse;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.soundcharts.dto.SoundchartsSearchResponse;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.soundcharts.dto.SoundchartsSongResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test unitaire basé sur de vrais payloads Soundcharts (src/test/resources/soundcharts/).
 * Vérifie la désérialisation Jackson (DTO) puis le mapping vers EnrichedTrackMetadata.
 */
class SoundchartsResponseParsingTest {

    private final JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();

    private SoundchartsSearchResponse search;
    private SoundchartsSongResponse metadata;

    private SoundchartsApiClient apiClient;
    private SoundchartsMusicMetadataAdapter adapter;

    @BeforeEach
    void setUp() throws IOException {
        search   = mapper.readValue(resource("soundcharts/song-search-agolo.json"),   SoundchartsSearchResponse.class);
        metadata = mapper.readValue(resource("soundcharts/song-metadata-agolo.json"), SoundchartsSongResponse.class);

        apiClient = mock(SoundchartsApiClient.class);
        adapter = new SoundchartsMusicMetadataAdapter(apiClient, 5);
    }

    @Test
    void shouldDeserializeSearchResponse() {
        assertThat(search.items()).hasSize(20);
        assertThat(search.page().total()).isEqualTo(24);

        var first = search.items().getFirst();
        assertThat(first.uuid()).isEqualTo("11e8262e-eab0-1e12-b8ac-aa1c026db3d8");
        assertThat(first.name()).isEqualTo("Agolo");
        assertThat(first.creditName()).isEqualTo("Angelique Kidjo");
        // Items de recherche : pas de bloc "artists" → primaryArtist() retombe sur creditName.
        assertThat(first.artists()).isNull();
        assertThat(first.primaryArtist()).isEqualTo("Angelique Kidjo");
        assertThat(first.releaseYear()).isEqualTo(1994);
    }

    @Test
    void shouldDeserializeSongMetadata() {
        var track = metadata.object();

        assertThat(track.uuid()).isEqualTo("11e8262e-eab0-1e12-b8ac-aa1c026db3d8");
        assertThat(track.name()).isEqualTo("Agolo");
        assertThat(track.primaryArtist()).isEqualTo("Angelique Kidjo");

        // isrc est un OBJET {value, countryCode, countryName}.
        assertThat(track.isrc().value()).isEqualTo("GBAPA9400001");
        assertThat(track.isrcValue()).isEqualTo("GBAPA9400001");
        assertThat(track.countryCode()).isEqualTo("GB");

        // genres : root + sub[].
        assertThat(track.genres()).hasSize(4);
        assertThat(track.genres().getFirst().root()).isEqualTo("electro");
        assertThat(track.genres().getFirst().sub()).containsExactly("dance", "electronic");

        // labels : liste d'objets.
        assertThat(track.primaryLabel()).isEqualTo("Island");

        // duration en secondes → durationMs() retourne des ms.
        assertThat(track.duration()).isEqualTo(288L);
        assertThat(track.durationMs()).isEqualTo(288_000L);

        // Bloc audio.
        assertThat(track.audio().tempo()).isEqualTo(93.95);
        assertThat(track.audio().key()).isEqualTo(0);
        assertThat(track.audio().mode()).isEqualTo(1);
    }

    @Test
    void shouldDeserializeLyricsAnalysis() throws IOException {
        var response = mapper.readValue(
                resource("soundcharts/song-lyrics-agolo.json"), SoundchartsLyricsResponse.class);

        assertThat(response.object()).isNotNull();
        assertThat(response.object().topics()).containsExactly("africa", "dance", "joy");
        assertThat(response.object().mood()).isEqualTo("uplifting");
        assertThat(response.object().sentiment()).isEqualTo("positive");
        assertThat(response.object().allThemes())
                .containsExactlyInAnyOrder("africa", "dance", "joy", "celebration", "cultural identity");
    }

    @Test
    void shouldEnrichEndToEndFromRealPayloads() {
        when(apiClient.searchSongByName(anyString(), anyInt(), anyInt())).thenReturn(search);
        when(apiClient.getSongMetadata(eq("11e8262e-eab0-1e12-b8ac-aa1c026db3d8"))).thenReturn(metadata);
        when(apiClient.getLyricsAnalysis(anyString())).thenReturn(null);

        var result = adapter.enrich("Angelique Kidjo", "Agolo");

        assertThat(result).isInstanceOf(EnrichmentResult.Success.class);
        var data = result.data();
        assertThat(data.sourceId()).isEqualTo("11e8262e-eab0-1e12-b8ac-aa1c026db3d8");
        assertThat(data.artist()).isEqualTo("Angelique Kidjo");
        assertThat(data.title()).isEqualTo("Agolo");
        assertThat(data.genres()).containsExactly("electro", "pop", "rock", "traditional");
        assertThat(data.styles()).containsExactly("dance", "electronic", "pop", "rock", "worldwide");
        assertThat(data.label()).isEqualTo("Island");
        assertThat(data.isrc()).isEqualTo("GBAPA9400001");
        assertThat(data.country()).isEqualTo("GB");
        assertThat(data.tags()).containsExactly("soundcharts");
        assertThat(data.releaseYear()).isEqualTo(1994);
        assertThat(data.durationMs()).isEqualTo(288_000L);

        var audio = data.audioFeatures();
        assertThat(audio).isNotNull();
        assertThat(audio.bpm()).isEqualTo(93.95);
        assertThat(audio.danceability()).isEqualTo(0.7);
        assertThat(audio.energy()).isEqualTo(0.81);
        assertThat(audio.valence()).isEqualTo(0.84);
        assertThat(audio.musicalKey()).isEqualTo("C");    // key=0
        assertThat(audio.mode()).isEqualTo("major");       // mode=1
        assertThat(audio.timeSignature()).isEqualTo(4);
    }

    private InputStream resource(String path) {
        return Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream(path),
                "Ressource introuvable : " + path);
    }
}
