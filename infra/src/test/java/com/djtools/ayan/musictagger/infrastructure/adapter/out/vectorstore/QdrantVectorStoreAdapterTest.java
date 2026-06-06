package com.djtools.ayan.musictagger.infrastructure.adapter.out.vectorstore;

import com.djtools.ayan.musictagger.domain.model.AudioFeatures;
import com.djtools.ayan.musictagger.domain.model.EnrichedTrackMetadata;
import com.djtools.ayan.musictagger.domain.model.SimilarTrackResult;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QdrantVectorStoreAdapterTest {

    @Mock EmbeddingStore<TextSegment> embeddingStore;
    @Mock EmbeddingModel embeddingModel;

    private QdrantVectorStoreAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new QdrantVectorStoreAdapter(embeddingStore, embeddingModel, 0.7);
        when(embeddingModel.embed(anyString()))
                .thenReturn(Response.from(Embedding.from(new float[768])));
    }

    private EnrichedTrackMetadata sampleTrack() {
        return new EnrichedTrackMetadata(
                "sp123", "Daft Punk", "Around The World", "Homework",
                List.of("Electronic", "House"), List.of("French House"), "Virgin", "FR",
                "ISRC123", List.of(), 1997, 82, 420000L,
                new AudioFeatures(0.8, 0.9, 0.7, null, null, null, 121.0, "Am", "minor", null, null, null),
                null, null
        );
    }

    @Test
    void store_shouldAddEmbeddingWithTextSegment() {
        adapter.store(sampleTrack());

        String expectedId = UUID.nameUUIDFromBytes("sp123".getBytes()).toString();
        verify(embeddingStore).remove(expectedId);
        verify(embeddingStore).add(any(Embedding.class), any(TextSegment.class));
    }

    @Test
    void buildEmbeddingText_shouldContainTrackInfo() {
        String text = adapter.buildEmbeddingText(sampleTrack());

        assertThat(text).contains("Around The World");
        assertThat(text).contains("Daft Punk");
        assertThat(text).contains("Electronic, House");
        assertThat(text).contains("121.0 BPM");
        assertThat(text).contains("Popularity: 82");
    }

    @Test
    void buildMetadata_shouldContainRequiredFields() {
        Map<String, Object> metadata = adapter.buildMetadata(sampleTrack());

        assertThat(metadata).containsEntry("sourceId", "sp123");
        assertThat(metadata).containsEntry("artist", "Daft Punk");
        assertThat(metadata).containsEntry("title", "Around The World");
        assertThat(metadata).containsEntry("genres", "Electronic,House");
        assertThat(metadata).containsEntry("bpm", 121.0);
        assertThat(metadata).containsEntry("energy", 0.9);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findSimilar_shouldMapMatchesToResults() {
        var meta = Metadata.from(Map.of(
                "sourceId", "sp456", "artist", "Bicep", "title", "Glue",
                "genres", "Electronic", "releaseYear", "2017", "popularity", "75",
                "bpm", "130.0", "energy", "0.85"
        ));
        var segment = TextSegment.from("Track: Glue by Bicep.", meta);
        var match = new EmbeddingMatch<>(0.92, "sp456", Embedding.from(new float[768]), segment);
        var searchResult = new EmbeddingSearchResult<>(List.of(match));
        when(embeddingStore.search(any(EmbeddingSearchRequest.class))).thenReturn(searchResult);

        List<SimilarTrackResult> results = adapter.findSimilar("electronic dance", 5);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().track().artist()).isEqualTo("Bicep");
        assertThat(results.getFirst().similarityScore()).isEqualTo(0.92);
    }
}
