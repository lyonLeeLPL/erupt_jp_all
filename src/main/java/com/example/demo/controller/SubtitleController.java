package com.example.demo.controller;

import com.example.demo.model.subtitle.VideoInfoVO;
import com.example.demo.utils.VttParser;
import com.example.demo.utils.YtDlpExtraUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;

@RestController
@RequestMapping("/api/video")
public class SubtitleController {

    @Autowired
    private YtDlpExtraUtils ytDlpUtils;

    @Autowired
    private VttParser vttParser;

    @GetMapping("/info")
    public VideoInfoVO getVideoInfo(@RequestParam("url") String url) {
        VideoInfoVO vo = new VideoInfoVO();
        try {
            // 1. Get Metadata from yt-dlp
            JsonNode meta = ytDlpUtils.getVideoInfo(url);

            String videoId = meta.get("id").asText();
            vo.setVideoId(videoId);
            vo.setTitle(meta.get("title").asText());
            vo.setDuration(meta.get("duration").asLong());

            // 2. Download VTT using yt-dlp (No more HTTP 429)
            String vttContent = ytDlpUtils.downloadSubtitleContent(videoId, url);

            // 3. Parse and Set
            if (vttContent != null) {
                vo.setSubtitles(vttParser.parse(vttContent));
            } else {
                vo.setSubtitles(new ArrayList<>());
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error processing video: " + e.getMessage());
        }
        return vo;
    }

    // findSubtitleUrl and getUrlFormat are no longer needed

}
