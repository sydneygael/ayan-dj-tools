package com.djtools.ayan.musictagger.infrastructure.adapter.out.musicbrainz;

import com.djtools.ayan.musictagger.infrastructure.adapter.out.musicbrainz.dto.MBRecordingSearchResponse;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

public interface MusicBrainzApiClient {

    @GetExchange("/ws/2/recording")
    MBRecordingSearchResponse searchRecordings(
            @RequestParam("query") String query,
            @RequestParam("fmt") String format,
            @RequestParam("limit") int limit
    );
}
