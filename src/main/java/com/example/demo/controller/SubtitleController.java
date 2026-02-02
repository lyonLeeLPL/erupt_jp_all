package com.example.demo.controller;

import com.example.demo.model.subtitle.VideoInfoVO;
import com.example.demo.utils.SubtitleParser;
import com.example.demo.utils.YouTubeApiUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/video")
public class SubtitleController {

    @Autowired
    private YouTubeApiUtils youTubeApiUtils;

    @Autowired
    private SubtitleParser subtitleParser;

    @GetMapping("/info")
    public VideoInfoVO getVideoInfo(@RequestParam("url") String url) {
        VideoInfoVO vo = new VideoInfoVO();
        try {
            // Extract Video ID from URL
            String videoId = extractVideoId(url);
            if (videoId == null)
                throw new RuntimeException("Invalid YouTube URL");

            // 1. Get Metadata from YouTube API
            JsonNode snippet = youTubeApiUtils.getVideoDetails(videoId).get("snippet");
            JsonNode contentDetails = youTubeApiUtils.getVideoDetails(videoId).get("contentDetails");

            vo.setVideoId(videoId);
            vo.setTitle(snippet.get("title").asText());
            // Parse duration (PT21M3S) - Simplified for now or use library
            vo.setDuration(0); // TODO: parse ISO8601 duration

            // 2. Find Subtitle Track ID
            String captionId = youTubeApiUtils.getCaptionId(videoId, "en");

            // 3. Download & Parse
            if (captionId != null) {
                String vttContent = youTubeApiUtils.downloadCaption(captionId);
                if (vttContent != null) {
                    vo.setSubtitles(subtitleParser.parseVtt(vttContent));
                }
            } else {
                vo.setSubtitles(new ArrayList<>()); // No subtitles found
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error processing video: " + e.getMessage());
        }
        return vo;
    }

    private String extractVideoId(String url) {
        String pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200C\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*";
        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(url);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }
}
