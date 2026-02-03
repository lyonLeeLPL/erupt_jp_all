package com.example.demo.controller;

import com.example.demo.ExampleApplication;
import com.example.demo.model.subtitle.VideoInfoVO;
import com.example.demo.service.SubtitleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/info")
    public VideoInfoVO getVideoInfo(@RequestParam("url") String url) {
        logger.info("Request received for URL: " + url);
        VideoInfoVO vo = subtitleService.processVideo(url);
        logger.info("Successfully processed video: " + vo.getTitle());
        return vo;
    }
}
