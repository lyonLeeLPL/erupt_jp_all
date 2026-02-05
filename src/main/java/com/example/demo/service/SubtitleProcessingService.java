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
                // The original code had a redundant if here. If NextStartTime <=
                // CurrentStartTime,
                // we should ideally clamp or do nothing, not re-check NextStartTime >
                // CurrentStartTime.
                // For now, keeping the original behavior of doing nothing if NextStartTime <=
                // CurrentStartTime
                // as the provided edit was redundant.
            }
        }

        // Optional: Extend last item? User said "Retain original".
        // We leave the last item as is.
    }

    /**
     * Intelligently split long subtitles (> 50 chars) into smaller segments.
     * Splits based on:
     * 1. Punctuation (comma, period, question mark) - High Priority
     * 2. Spaces - Medium Priority
     * 3. Length - Last Resort
     * 
     * Time is redistributed proportionally to text length.
     */
    public List<VideoInfoVO.SubtitleItemVO> splitLongSubtitles(List<VideoInfoVO.SubtitleItemVO> list) {
        List<VideoInfoVO.SubtitleItemVO> result = new java.util.ArrayList<>();
        int MAX_LEN = 50;

        for (VideoInfoVO.SubtitleItemVO item : list) {
            String text = item.getOriginal();
            if (text == null || text.length() <= MAX_LEN) {
                result.add(item); // Keep as is
                continue;
            }

            // Needs splitting
            List<String> segments = splitTextSmartly(text, MAX_LEN);

            // Distribute time EQUALLY (Per User Request)
            long totalDuration = item.getEndTime() - item.getStartTime();
            long currentStart = item.getStartTime();
            int segmentCount = segments.size();
            long durationPerSegment = totalDuration / segmentCount;

            for (int i = 0; i < segments.size(); i++) {
                String segText = segments.get(i);

                // Calculate end time for this segment
                long segEnd;
                if (i == segments.size() - 1) {
                    segEnd = item.getEndTime(); // Secure exact end time for last segment
                } else {
                    segEnd = currentStart + durationPerSegment;
                }

                VideoInfoVO.SubtitleItemVO newItem = new VideoInfoVO.SubtitleItemVO(
                        item.getIndex(),
                        currentStart,
                        segEnd,
                        segText);

                // Handle Translation
                if (i == 0) {
                    newItem.setTranslation(item.getTranslation());
                } else {
                    newItem.setTranslation("(...) " + (item.getTranslation() != null ? item.getTranslation() : ""));
                }

                result.add(newItem);

                // Advance start time
                currentStart = segEnd;
            }
        }

        // Re-index
        for (int i = 0; i < result.size(); i++) {
            result.get(i).setIndex(i + 1);
        }

        logger.info("Split subtitles from " + list.size() + " to " + result.size());
        return result;
    }

    private List<String> splitTextSmartly(String text, int maxLen) {
        List<String> parts = new java.util.ArrayList<>();

        while (text.length() > maxLen) {
            // Find best split point near maxLen (search backwards from maxLen)
            int splitIndex = -1;

            // Search window: check last 20 chars of the allowance
            int scanStart = Math.max(0, maxLen - 20);
            int scanEnd = maxLen;

            // Priority 1: Major Punctuation (。 ? ! ; )
            // Priority 2: Minor Punctuation ( , ， 、 )
            // Priority 3: Space

            String snippet = text.substring(scanStart, scanEnd);

            // Regex for sentence stoppers
            java.util.regex.Matcher m1 = java.util.regex.Pattern.compile("[。？！?!;；]").matcher(snippet);
            if (m1.find()) { // Take the last one found
                splitIndex = scanStart + m1.end(); // Split AFTER the punctuation
            }

            if (splitIndex == -1) {
                java.util.regex.Matcher m2 = java.util.regex.Pattern.compile("[,，、]").matcher(snippet);
                if (m2.find()) {
                    splitIndex = scanStart + m2.end();
                }
            }

            if (splitIndex == -1) {
                int spaceIdx = snippet.lastIndexOf(' ');
                if (spaceIdx != -1) {
                    splitIndex = scanStart + spaceIdx + 1;
                }
            }

            // Fallback: Hard split at maxLen
            if (splitIndex == -1) {
                splitIndex = maxLen;
            }

            parts.add(text.substring(0, splitIndex).trim());
            text = text.substring(splitIndex).trim();
        }

        if (!text.isEmpty()) {
            parts.add(text);
        }

        return parts;
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
