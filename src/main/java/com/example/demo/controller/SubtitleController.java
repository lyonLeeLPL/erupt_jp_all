package com.example.demo.controller;

import com.example.demo.ExampleApplication;
import com.example.demo.model.subtitle.VideoInfoVO;
import com.example.demo.utils.VttParser;
import com.example.demo.utils.YtDlpExtraUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/video")
public class SubtitleController {
    private static final Logger logger = Logger.getLogger(ExampleApplication.class.getName());

    @Autowired
    private YtDlpExtraUtils ytDlpUtils;

    @Autowired
    private VttParser vttParser;

    @Value("${erupt.youtube-path}")
    private String youtubePath;

    @GetMapping("/info")
    public VideoInfoVO getVideoInfo(@RequestParam("url") String url) {
        VideoInfoVO vo = new VideoInfoVO();
        try {
            // 1. Get Metadata
            JsonNode meta = ytDlpUtils.getVideoInfo(url);
            vo.setVideoId(meta.get("id").asText());
            vo.setTitle(meta.get("title").asText());
            vo.setDuration(meta.get("duration").asLong());

            // 2. Download VTT
            java.io.File vttFile = ytDlpUtils.downloadSubtitle(url, meta);

            // 3. Parse and Set Raw Content
            // Need to reload file list because yt-dlp might have downloaded multiple
            String tempDir = youtubePath + java.io.File.separator + "sublingo";
            java.io.File dir = new java.io.File(tempDir);
            String videoId = vo.getVideoId();

            if (dir.exists()) {
                // Scan all VTT files for this video
                java.io.File[] files = dir.listFiles((d, name) -> name.startsWith(videoId) && name.endsWith(".vtt"));

                java.io.File bestSource = null;
                java.io.File bestTarget = null;

                if (files != null) {
                    for (java.io.File f : files) {
                        String name = f.getName();
                        // Extract lang: videoId.LANG.vtt
                        String suffix = name.substring(videoId.length());
                        if (suffix.startsWith("."))
                            suffix = suffix.substring(1);

                        String lang = suffix.replace(".vtt", "").toLowerCase();
                        logger.info("Found VTT: " + name + " (Lang: " + lang + ")");

                        // Pick Source (Priorty: ja > en)
                        if (lang.startsWith("ja")) {
                            bestSource = f;
                        } else if (lang.startsWith("en")
                                && (bestSource == null || !bestSource.getName().contains(".ja"))) {
                            if (bestSource == null)
                                bestSource = f;
                        }

                        // Pick Target (Priority: zh)
                        if (lang.startsWith("zh")) {
                            bestTarget = f;
                        }
                    }
                }

                // Lists for parsing
                java.util.List<VideoInfoVO.SubtitleItemVO> sourceList = new ArrayList<>();
                java.util.List<VideoInfoVO.SubtitleItemVO> targetList = new ArrayList<>();

                if (bestSource != null) {
                    logger.info("Parsing Source: " + bestSource.getName());
                    sourceList = vttParser.parse(bestSource);
                } else {
                    logger.warning("No Source VTT found!");
                }

                // OPTIMIZATION: Filter out purely numeric 'garbage' lines (e.g. "1", "11",
                // "39")
                // Rule: If text (no whitespace) is all digits and length <= 5, remove it.
                sourceList.removeIf(item -> {
                    String clean = item.getOriginal().replaceAll("\\s+", "");
                    return clean.matches("\\d+") && clean.length() <= 5;
                });

                if (bestTarget != null) {
                    logger.info("Parsing Target: " + bestTarget.getName());
                    targetList = vttParser.parse(bestTarget);
                } else {
                    logger.warning("No Target VTT found!");
                }

                // Merge Logic: Align Target to Source
                if (!sourceList.isEmpty() && !targetList.isEmpty()) {
                    logger.info("Merging subtitles...");
                    for (VideoInfoVO.SubtitleItemVO sourceItem : sourceList) {
                        long start1 = sourceItem.getStartTime();
                        long end1 = sourceItem.getEndTime();

                        // Find best matching target
                        for (VideoInfoVO.SubtitleItemVO targetItem : targetList) {
                            long start2 = targetItem.getStartTime();
                            long end2 = targetItem.getEndTime();

                            // Calc overlap
                            long startOverlap = Math.max(start1, start2);
                            long endOverlap = Math.min(end1, end2);
                            long overlapCallback = endOverlap - startOverlap;

                            // 500ms threshold
                            if (overlapCallback > 500) {
                                String existing = sourceItem.getTranslation();
                                String newTrans = targetItem.getOriginal();

                                if (existing == null || existing.isEmpty()) {
                                    sourceItem.setTranslation(newTrans);
                                }
                            }
                        }
                    }
                }

                // Generate Merged VTT Content
                StringBuilder vttBuilder = new StringBuilder();
                vttBuilder.append("WEBVTT\n\n");

                for (VideoInfoVO.SubtitleItemVO item : sourceList) {
                    vttBuilder.append(item.getIndex()).append("\n");
                    vttBuilder.append(formatTime(item.getStartTime())).append(" --> ")
                            .append(formatTime(item.getEndTime())).append("\n");
                    vttBuilder.append(item.getOriginal()).append("\n");
                    if (item.getTranslation() != null && !item.getTranslation().isEmpty()) {
                        vttBuilder.append(item.getTranslation()).append("\n");
                    }
                    vttBuilder.append("\n");
                }

                String mergedContent = vttBuilder.toString();

                // Save to file
                try {
                    java.io.File mergedFile = new java.io.File(dir, videoId + ".merged.vtt");
                    java.nio.file.Files.writeString(mergedFile.toPath(), mergedContent,
                            java.nio.charset.StandardCharsets.UTF_8);
                    logger.info("Saved Merged VTT to: " + mergedFile.getAbsolutePath());
                } catch (Exception e) {
                    logger.warning("Failed to save merged VTT: " + e.getMessage());
                }

                vo.setSubtitles(sourceList);
                vo.setRawVtt(mergedContent); // Return the merged VTT as raw
                vo.setTranslationVtt(null);
            } else {
                vo.setSubtitles(new ArrayList<>());
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error processing video: " + e.getMessage());
        }

        logger.info("success:" + vo.getTitle());
        return vo;
    }

    private String formatTime(long totalMs) {
        long h = totalMs / 3600000;
        long m = (totalMs % 3600000) / 60000;
        long s = (totalMs % 60000) / 1000;
        long ms = totalMs % 1000;
        return String.format("%02d:%02d:%02d.%03d", h, m, s, ms);
    }
}
