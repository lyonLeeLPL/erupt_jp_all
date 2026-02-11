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
            }
        }
    }

    /**
     * Adaptive Strategy: Identify "Auxiliary" subtitles (noise, short fillers)
     * and merge them into the previous "Main" subtitle.
     * This reclaims time for the main content and reduces clutter.
     */
    public List<VideoInfoVO.SubtitleItemVO> mergeAuxiliarySubtitles(List<VideoInfoVO.SubtitleItemVO> list) {
        if (list == null || list.isEmpty())
            return new java.util.ArrayList<>();

        List<VideoInfoVO.SubtitleItemVO> result = new java.util.ArrayList<>();
        VideoInfoVO.SubtitleItemVO lastMain = null;

        for (VideoInfoVO.SubtitleItemVO item : list) {
            boolean isAux = isAuxiliary(item.getOriginal());

            if (lastMain != null && isAux) {
                // MERGE STRATEGY:
                // 1. Extend LastMain time to cover this Aux item
                lastMain.setEndTime(Math.max(lastMain.getEndTime(), item.getEndTime()));

                // 2. Append text (Optional, maybe with space? or just ignore text if it's pure
                // noise?)
                // User said: "Merge 204 as tail tone".
                // We append it to keep transcript complete but visually cleaner.
                // lastMain.setOriginal(lastMain.getOriginal() + " " + item.getOriginal());

                // 3. What about Translation?
                // Usually Aux translation is "..." or empty. We ignore it.

                logger.info("Merged Auxiliary [" + item.getOriginal() + "] into [" + lastMain.getOriginal() + "]");
                // We do NOT add 'item' to result, effectively "deleting" it as a standalone
                // entry.
            } else {
                // Identify as new Main
                result.add(item);
                lastMain = item;
            }
        }

        // Re-index
        for (int i = 0; i < result.size(); i++) {
            result.get(i).setIndex(i + 1);
        }

        return result;
    }

    private boolean isAuxiliary(String text) {
        if (text == null)
            return true;
        String clean = text.trim();
        // Rule 1: Very short (<= 2 chars)
        if (clean.length() <= 2)
            return true;

        // Rule 2: Specific Filler Words (Japanese/Chinese/English)
        // Adjust regex as needed
        if (clean.matches("^(?i)(ah|oh|um|uh|ま|あの|え|えっと|嗯|呃|那个)$"))
            return true;

        return false;
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

            // Distribute time EQUALLY
            long totalDuration = item.getEndTime() - item.getStartTime();
            long currentStart = item.getStartTime();
            int segmentCount = segments.size();
            long durationPerSegment = totalDuration / segmentCount;

            // Split Translation if it exists
            List<String> transSegments = null;
            if (item.getTranslation() != null && !item.getTranslation().isEmpty()) {
                transSegments = splitTranslationByParts(item.getTranslation(), segmentCount);
            }

            for (int i = 0; i < segments.size(); i++) {
                String segText = segments.get(i);

                // Calculate end time
                long segEnd;
                if (i == segments.size() - 1) {
                    segEnd = item.getEndTime();
                } else {
                    segEnd = currentStart + durationPerSegment;
                }

                VideoInfoVO.SubtitleItemVO newItem = new VideoInfoVO.SubtitleItemVO(
                        item.getIndex(),
                        currentStart,
                        segEnd,
                        segText);

                // Handle Translation
                if (transSegments != null && i < transSegments.size()) {
                    newItem.setTranslation(transSegments.get(i));
                } else {
                    newItem.setTranslation(""); // Should not happen if split correctly
                }

                result.add(newItem);
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
     * Heuristic to split translation text into K roughly equal parts.
     * Tries to respect punctuation first.
     */
    private List<String> splitTranslationByParts(String text, int parts) {
        List<String> result = new java.util.ArrayList<>();
        if (parts <= 1) {
            result.add(text);
            return result;
        }

        // 1. Try splitting by punctuation
        // Note: This is naive. Ideally we used the same smart split as original,
        // but here we are constrained by 'parts' count.

        // Strategy: Naive length division
        // We divide the string into 'parts' chunks of equal characters.
        // And then try to snap to the nearest space/punctuation.

        int length = text.length();
        int chunkLen = length / parts;
        int currentPos = 0;

        for (int i = 0; i < parts; i++) {
            if (i == parts - 1) {
                // Last part takes the rest
                result.add(text.substring(currentPos).trim());
            } else {
                int target = currentPos + chunkLen;
                // Look for delimiter around target (+/- 5 chars)
                int split = findBestSplitPoint(text, target, 5);
                if (split == -1)
                    split = target; // Fallback to hard cut

                result.add(text.substring(currentPos, split).trim());
                currentPos = split;
            }
        }
        return result;
    }

    private int findBestSplitPoint(String text, int target, int radius) {
        if (target >= text.length())
            return text.length();

        // Search outwards from target
        for (int offset = 0; offset <= radius; offset++) {
            // Check right
            int right = target + offset;
            if (right < text.length() && isDelimiter(text.charAt(right)))
                return right + 1;

            // Check left
            int left = target - offset;
            if (left > 0 && isDelimiter(text.charAt(left)))
                return left + 1;
        }
        return -1;
    }

    private boolean isDelimiter(char c) {
        return " ,，.。!！?？;；、".indexOf(c) != -1;
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
