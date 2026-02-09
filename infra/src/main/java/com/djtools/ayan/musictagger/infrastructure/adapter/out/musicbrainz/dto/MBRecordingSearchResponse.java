package com.djtools.ayan.musictagger.infrastructure.adapter.out.musicbrainz.dto;

import java.util.List;

public record MBRecordingSearchResponse(int count, int offset, List<MBRecording> recordings) {}
