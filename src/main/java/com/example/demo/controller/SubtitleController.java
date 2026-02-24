package com.example.demo.controller;

import com.example.demo.ExampleApplication;
import com.example.demo.model.subtitle.VideoInfoVO;
import com.example.demo.service.SubtitleFileService;
import com.example.demo.service.SubtitleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Controller for Subtitle and Video Information.
 * Refactored to delegate business logic to SubtitleService.
 */
@RestController
@RequestMapping("/api/video")
public class SubtitleController {

    private static final Logger logger = Logger.getLogger(ExampleApplication.class.getName());

    @Autowired
    private SubtitleService subtitleService;

    @Autowired
    private SubtitleFileService subtitleFileService;

    @GetMapping("/info")
    public VideoInfoVO getVideoInfo(@RequestParam("url") String url) {
        logger.info("Request received for URL: " + url);
        VideoInfoVO vo = subtitleService.processVideo(url);
        logger.info("Successfully processed video: " + vo.getTitle());
        return vo;
    }

    @GetMapping("/subtitle/download")
    public ResponseEntity<Resource> downloadSubtitle(
            @RequestParam("videoId") String videoId,
            @RequestParam("type") String type) {

        String url = "https://www.youtube.com/watch?v=" + videoId;
        getVideoInfo(url);

        java.io.File file = subtitleService.getSubtitleFile(videoId, type);

        if (file == null || !file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getName() + "\"")
                .contentType(MediaType.parseMediaType("application/x-subrip"))
                .body(resource);
    }

    /**
     * Stream Audio with Range Support (206 Partial Content)
     */
    @GetMapping("/audio/{videoId}")
    public ResponseEntity<ResourceRegion> streamAudio(
            @PathVariable String videoId,
            @RequestHeader HttpHeaders headers) throws IOException {

        // 1. Get Audio File (Download if missing - Blocking for MVP)
        File audioFile = subtitleFileService.getAudioFile(videoId);
        if (audioFile == null) {
            audioFile = subtitleFileService.downloadAudio(videoId);
        }

        if (audioFile == null || !audioFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        // 2. Prepare Resource
        Resource resource = new FileSystemResource(audioFile);
        long contentLength = resource.contentLength();

        // 3. Create ResourceRegion based on Range Header
        HttpRange range = headers.getRange().isEmpty() ? null : headers.getRange().get(0);
        ResourceRegion region;

        if (range != null) {
            long start = range.getRangeStart(contentLength);
            long end = range.getRangeEnd(contentLength);
            long rangeLength = Math.min(1024 * 1024, end - start + 1); // 1MB chunk or rest
            region = new ResourceRegion(resource, start, rangeLength);
        } else {
            long rangeLength = Math.min(1024 * 1024, contentLength); // 1MB default chunk
            region = new ResourceRegion(resource, 0, rangeLength);
        }

        // 4. Return 206 Partial Content
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(MediaType.parseMediaType("audio/mpeg")) // mp3
                .body(region);
    }
}
