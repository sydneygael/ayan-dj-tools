package com.djtools.ayan.musictagger.domain.service;

import com.djtools.ayan.musictagger.domain.model.EnrichedTrackMetadata;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

public class MetadataMerger {

    public EnrichedTrackMetadata merge(List<EnrichedTrackMetadata> results) {
        if (results.isEmpty()) {
            throw new IllegalArgumentException("Cannot merge empty results");
        }
        if (results.size() == 1) {
            return results.getFirst();
        }

        return new EnrichedTrackMetadata(
                firstNonNull(results, EnrichedTrackMetadata::sourceId),
                firstNonNull(results, EnrichedTrackMetadata::artist),
                firstNonNull(results, EnrichedTrackMetadata::title),
                firstNonNull(results, EnrichedTrackMetadata::album),
                mergeLists(results, EnrichedTrackMetadata::genres),
                mergeLists(results, EnrichedTrackMetadata::styles),
                firstNonNull(results, EnrichedTrackMetadata::label),
                firstNonNull(results, EnrichedTrackMetadata::country),
                firstNonNull(results, EnrichedTrackMetadata::isrc),
                mergeLists(results, EnrichedTrackMetadata::tags),
                firstNonZero(results, EnrichedTrackMetadata::releaseYear),
                firstNonZero(results, EnrichedTrackMetadata::popularity),
                firstNonZero(results, EnrichedTrackMetadata::durationMs),
                firstNonNull(results, EnrichedTrackMetadata::audioFeatures)
        );
    }

    private <T> T firstNonNull(List<EnrichedTrackMetadata> results, Function<EnrichedTrackMetadata, T> extractor) {
        for (var r : results) {
            T value = extractor.apply(r);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private int firstNonZero(List<EnrichedTrackMetadata> results, ToIntFunction<EnrichedTrackMetadata> extractor) {
        for (var r : results) {
            int value = extractor.applyAsInt(r);
            if (value != 0) {
                return value;
            }
        }
        return 0;
    }

    private long firstNonZero(List<EnrichedTrackMetadata> results, ToLongFunction<EnrichedTrackMetadata> extractor) {
        for (var r : results) {
            long value = extractor.applyAsLong(r);
            if (value != 0) {
                return value;
            }
        }
        return 0;
    }

    private List<String> mergeLists(List<EnrichedTrackMetadata> results, Function<EnrichedTrackMetadata, List<String>> extractor) {
        var merged = new LinkedHashSet<String>();
        for (var r : results) {
            List<String> values = extractor.apply(r);
            if (values != null) {
                merged.addAll(values);
            }
        }
        return List.copyOf(merged);
    }
}
