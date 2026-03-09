package com.djtools.ayan.musictagger.infrastructure.adapter.out.vectorstore;

import com.djtools.ayan.musictagger.domain.model.AudioFeatures;
import com.djtools.ayan.musictagger.domain.model.EnrichedTrackMetadata;
import com.djtools.ayan.musictagger.domain.model.SimilarTrackResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QdrantVectorStoreAdapterTest {

    @Mock VectorStore vectorStore;

    private QdrantVectorStoreAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new QdrantVectorStoreAdapter(vectorStore, 0.7);
    }

    private EnrichedTrackMetadata sampleTrack() {
        return new EnrichedTrackMetadata(
                "sp123", "Daft Punk", "Around The World", "Homework",
                List.of("Electronic", "House"), List.of("French House"), "Virgin", "FR",
                "ISRC123", List.of(), 1997, 82, 420000,
                new AudioFeatures(0.8, 0.9, 0.7, null, null, null, 121.0, "Am", "minor", null)
        );
    }

    @Test
    void store_shouldConvertToDocumentWithCorrectId() {
        adapter.store(sampleTrack());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());

        List<Document> docs = captor.getValue();
        assertThat(docs).hasSize(1);
        assertThat(docs.getFirst().getId()).isEqualTo("sp123");
    }

    @Test
    void store_shouldBuildRichEmbeddingText() {
        String text = adapter.buildEmbeddingText(sampleTrack());

        assertThat(text).contains("Around The World");
        assertThat(text).contains("Daft Punk");
        assertThat(text).contains("Electronic, House");
        assertThat(text).contains("121.0 BPM");
        assertThat(text).contains("Popularity: 82");
    }

    @Test
    void store_shouldIncludeMetadataFields() {
        Map<String, Object> metadata = adapter.buildMetadata(sampleTrack());

        assertThat(metadata).containsEntry("sourceId", "sp123");
        assertThat(metadata).containsEntry("artist", "Daft Punk");
        assertThat(metadata).containsEntry("title", "Around The World");
        assertThat(metadata).containsEntry("genres", "Electronic,House");
        assertThat(metadata).containsEntry("bpm", 121.0);
        assertThat(metadata).containsEntry("energy", 0.9);
    }

    @Test
    void findSimilar_shouldMapDocumentsToResults() {
        var doc = Document.builder()
                .id("sp456")
                .text("Track: Test")
                .metadata(Map.of(
                        "sourceId", "sp456", "artist", "Bicep", "title", "Glue",
                        "genres", "Electronic", "releaseYear", 2017, "popularity", 75,
                        "bpm", 130.0, "energy", 0.85
                ))
                .score(0.92)
                .build();
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc));

        List<SimilarTrackResult> results = adapter.findSimilar("electronic dance", 5);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().track().artist()).isEqualTo("Bicep");
        assertThat(results.getFirst().similarityScore()).isEqualTo(0.92);
    }
}
