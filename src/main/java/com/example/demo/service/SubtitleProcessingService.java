package com.example.demo.service;

import com.example.demo.model.subtitle.VideoInfoVO;
import com.example.demo.utils.SrtParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

/**
 * Service specifically for Processing Subtitle Logic
 * - Parsing (SRT)
 * - Filtering (Garbage removal)
 * - Merging (Dual language alignment)
 * - Generation (SRT Formatting)
 */
@Service
public class SubtitleProcessingService {

    private static final Logger logger = Logger.getLogger(SubtitleProcessingService.class.getName());

    @Autowired
    private SrtParser srtParser;

    /**
     * Parse SRT file and filter out garbage lines
     */
    public List<VideoInfoVO.SubtitleItemVO> parseAndFilter(File file) {
        if (file == null || !file.exists()) {
            return new java.util.ArrayList<>();
        }

        List<VideoInfoVO.SubtitleItemVO> list = srtParser.parse(file);

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
     * Rewrite subtitle end times to fill gaps between segments.
     * Rule: Current Subtitle End Time = Next Subtitle Start Time.
     * Prevents flickering and ensures continuous playback.
     */
    public void fillTimelineGaps(List<VideoInfoVO.SubtitleItemVO> list) {
        if (list == null || list.size() < 2) {
            return;
        }

        logger.info("Optimizing timeline for " + list.size() + " items...");

        for (int i = 0; i < list.size() - 1; i++) {
            VideoInfoVO.SubtitleItemVO current = list.get(i);
            VideoInfoVO.SubtitleItemVO next = list.get(i + 1);

            long NextStartTime = next.getStartTime();
            long CurrentStartTime = current.getStartTime();

            // Safety Check: Ensure we don't create invalid duration
            if (NextStartTime > CurrentStartTime) {
                current.setEndTime(NextStartTime);
            } else {
                // Next starts before current?? (Overlap or Sort error)
                // In this case, just keep original end time or clamp to next start?
                // Safer: Math.max(current.getEndTime(), NextStartTime) might be wrong if next
                // is fully inside.
                // Simple Fix: do nothing if invalid, or clamp if overlapping?
                // User request: "Fix negative time".
                // If overlaps, usually we want to cut the current one short to let next one
                // play.
                if (NextStartTime > CurrentStartTime) {
                    current.setEndTime(NextStartTime);
                }
            }
        }

        // Optional: Extend last item? User said "Retain original".
        // We leave the last item as is.
    }

    /**
     * Generate standard SRT string from subtitle list
     */
    public String generateSrtContent(List<VideoInfoVO.SubtitleItemVO> list) {
        StringBuilder srtBuilder = new StringBuilder();
        // SRT does not have a header like WEBVTT

        for (VideoInfoVO.SubtitleItemVO item : list) {
            srtBuilder.append(item.getIndex()).append("\n");
            srtBuilder.append(formatTime(item.getStartTime()))
                    .append(" --> ")
                    .append(formatTime(item.getEndTime()))
                    .append("\n");

            srtBuilder.append(item.getOriginal()).append("\n");

            if (item.getTranslation() != null && !item.getTranslation().isEmpty()) {
                srtBuilder.append(item.getTranslation()).append("\n");
            }
            srtBuilder.append("\n");
        }
        return srtBuilder.toString();
    }

    private String formatTime(long totalMs) {
        long h = totalMs / 3600000;
        long m = (totalMs % 3600000) / 60000;
        long s = (totalMs % 60000) / 1000;
        long ms = totalMs % 1000;
        // SRT uses comma and dot for seconds, standard SRT is comma 00:00:00,000
        return String.format("%02d:%02d:%02d,%03d", h, m, s, ms);
    }
}
