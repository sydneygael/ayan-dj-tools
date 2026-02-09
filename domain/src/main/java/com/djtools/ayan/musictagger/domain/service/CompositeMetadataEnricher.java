package com.djtools.ayan.musictagger.domain.service;

import com.djtools.ayan.musictagger.domain.model.EnrichedTrackMetadata;
import com.djtools.ayan.musictagger.domain.model.EnrichmentResult;
import com.djtools.ayan.musictagger.domain.port.in.MusicMetadataProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CompositeMetadataEnricher {

    private final List<MusicMetadataProvider> providers;
    private final MetadataMerger merger;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public CompositeMetadataEnricher(List<MusicMetadataProvider> providers) {
        this.providers = List.copyOf(providers);
        this.merger = new MetadataMerger();
    }

    public EnrichmentResult enrich(String artist, String title) {
        List<CompletableFuture<EnrichmentResult>> futures = providers.stream()
                .map(provider -> CompletableFuture.supplyAsync(
                        () -> provider.enrich(artist, title), executor)
                        .exceptionally(ex -> EnrichmentResult.error(ex.getMessage())))
                .toList();

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

        List<EnrichedTrackMetadata> successes = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (var future : futures) {
            EnrichmentResult result = future.join();
            if (result.isSuccess()) {
                successes.add(result.data());
            } else if (result instanceof EnrichmentResult.Error(String message)) {
                errors.add(message);
            }
        }

        if (successes.isEmpty()) {
            if (errors.isEmpty()) {
                return EnrichmentResult.notFound();
            }
            return EnrichmentResult.error(String.join("; ", errors));
        }

        return EnrichmentResult.success(merger.merge(successes));
    }
}
