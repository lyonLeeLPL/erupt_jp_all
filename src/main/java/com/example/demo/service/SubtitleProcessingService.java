package com.example.demo.service;

import com.example.demo.model.subtitle.VideoInfoVO;
import com.example.demo.utils.VttParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

/**
 * Service specifically for Processing Subtitle Logic
 * - Parsing
 * - Filtering (Garbage removal)
 * - Merging (Dual language alignment)
 * - Generation (VTT Formatting)
 */
@Service
public class SubtitleProcessingService {

    private static final Logger logger = Logger.getLogger(SubtitleProcessingService.class.getName());

    @Autowired
    private VttParser vttParser;

    /**
     * Parse VTT file and filter out garbage lines
     */
    public List<VideoInfoVO.SubtitleItemVO> parseAndFilter(File file) {
        if (file == null || !file.exists()) {
            return new java.util.ArrayList<>();
        }

        List<VideoInfoVO.SubtitleItemVO> list = vttParser.parse(file);

        // Filter Logic:
        // 1. Numeric noise (<= 5 digits)
        // 2. Sound effects like [laugh], [Music]
        list.removeIf(item -> {
            String clean = item.getOriginal().trim();

            // Numeric check
            String noSpace = clean.replaceAll("\\s+", "");
            boolean isNumericGarbage = noSpace.matches("\\d+") && noSpace.length() <= 5;

            // Bracket check
            boolean isBracketGarbage = clean.startsWith("[") && clean.endsWith("]");

            return isNumericGarbage || isBracketGarbage;
        });

        return list;
    }

    /**
     * Merge Target subtitles into Source based on timestamp proximity.
     * Logic: A match is found if start times are within 200ms.
     */
    public void mergeSubtitles(List<VideoInfoVO.SubtitleItemVO> sourceList,
            List<VideoInfoVO.SubtitleItemVO> targetList) {
        if (sourceList.isEmpty() || targetList.isEmpty()) {
            return;
        }

        logger.info("Merging subtitles... Source: " + sourceList.size() + ", Target: " + targetList.size());

        for (VideoInfoVO.SubtitleItemVO sourceItem : sourceList) {
            long start1 = sourceItem.getStartTime();

            for (VideoInfoVO.SubtitleItemVO targetItem : targetList) {
                long start2 = targetItem.getStartTime();

                // Tolerance: 200ms absolute difference
                if (Math.abs(start1 - start2) < 200) {
                    String existing = sourceItem.getTranslation();
                    String newTrans = targetItem.getOriginal(); // Target's "original" is the translation text

                    if (existing == null || existing.isEmpty()) {
                        sourceItem.setTranslation(newTrans);
                    }
                    // Assumes 1:1 mapping is sufficient
                    break;
                }
            }
        }
    }

    /**
     * Generate standard WEBVTT string from subtitle list
     */
    public String generateVttContent(List<VideoInfoVO.SubtitleItemVO> list) {
        StringBuilder vttBuilder = new StringBuilder();
        vttBuilder.append("WEBVTT\n\n");

        for (VideoInfoVO.SubtitleItemVO item : list) {
            vttBuilder.append(item.getIndex()).append("\n");
            vttBuilder.append(formatTime(item.getStartTime()))
                    .append(" --> ")
                    .append(formatTime(item.getEndTime()))
                    .append("\n");

            vttBuilder.append(item.getOriginal()).append("\n");

            if (item.getTranslation() != null && !item.getTranslation().isEmpty()) {
                vttBuilder.append(item.getTranslation()).append("\n");
            }
            vttBuilder.append("\n");
        }
        return vttBuilder.toString();
    }

    private String formatTime(long totalMs) {
        long h = totalMs / 3600000;
        long m = (totalMs % 3600000) / 60000;
        long s = (totalMs % 60000) / 1000;
        long ms = totalMs % 1000;
        return String.format("%02d:%02d:%02d.%03d", h, m, s, ms);
    }
}
