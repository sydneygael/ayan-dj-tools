package com.djtools.ayan.musictagger.infrastructure.adapter.out.discogs;

import com.djtools.ayan.musictagger.infrastructure.adapter.out.discogs.dto.DiscogsRelease;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.discogs.dto.DiscogsSearchResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

public interface DiscogsApiClient {

    @GetExchange("/database/search")
    DiscogsSearchResponse search(
            @RequestParam("q") String query,
            @RequestParam("type") String type,
            @RequestParam("per_page") int perPage
    );

    @GetExchange("/releases/{id}")
    DiscogsRelease getRelease(@PathVariable("id") long id);
}
