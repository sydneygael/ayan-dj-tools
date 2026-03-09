package com.djtools.ayan.musictagger.infrastructure.adapter.in.rest;

import com.djtools.ayan.musictagger.domain.model.TagPreview;
import com.djtools.ayan.musictagger.domain.model.TagWriteResult;
import com.djtools.ayan.musictagger.domain.port.out.AudioFileWriter;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final AudioFileWriter audioFileWriter;

    public TagController(AudioFileWriter audioFileWriter) {
        this.audioFileWriter = audioFileWriter;
    }

    @PostMapping("/apply")
    public TagWriteResult applyTags(@RequestBody ApplyTagsRequest request) {
        return audioFileWriter.writeTags(request.filepath(), request.tags());
    }

    @PostMapping("/preview")
    public TagPreview previewTags(@RequestBody ApplyTagsRequest request) {
        return audioFileWriter.previewChanges(request.filepath(), request.tags());
    }

    public record ApplyTagsRequest(String filepath, Map<String, String> tags) {}
}
