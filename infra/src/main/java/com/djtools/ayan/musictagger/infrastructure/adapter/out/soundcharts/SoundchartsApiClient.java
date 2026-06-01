package com.djtools.ayan.musictagger.infrastructure.adapter.out.soundcharts;

import com.djtools.ayan.musictagger.infrastructure.adapter.out.soundcharts.dto.SoundchartsSearchResponse;
import com.djtools.ayan.musictagger.infrastructure.adapter.out.soundcharts.dto.SoundchartsSongResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

public interface SoundchartsApiClient {

    @GetExchange("/api/v2/song/search/{term}")
    SoundchartsSearchResponse searchSongByName(
            @PathVariable("term") String term,
            @RequestParam(value = "offset", required = false) Integer offset,
            @RequestParam(value = "limit", required = false) Integer limit
    );

    @GetExchange("/api/v2.25/song/{uuid}")
    SoundchartsSongResponse getSongMetadata(@PathVariable("uuid") String uuid);
}
