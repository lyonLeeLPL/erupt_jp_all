package com.example.demo.utils;

import com.example.demo.model.subtitle.VideoInfoVO;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses SRT (SubRip) subtitle format.
 * Format:
 * 1
 * 00:00:01,000 --> 00:00:05,080
 * Text content
 * 
 * Empty line separates entries.
 */
@Component
public class SrtParser {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SrtParser.class);

    public List<VideoInfoVO.SubtitleItemVO> parse(File file) {
        List<VideoInfoVO.SubtitleItemVO> subtitles = new ArrayList<>();
        if (file == null || !file.exists())
            return subtitles;

        try (BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(file),
                java.nio.charset.StandardCharsets.UTF_8))) {

            String line;
            int step = 0; // 0=Index, 1=Time, 2=Text
            int index = 0;
            String timeLine = null;
            StringBuilder textBuilder = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) {
                    // End of segment
                    if (timeLine != null && textBuilder.length() > 0) {
                        addSubItem(subtitles, index, timeLine, textBuilder.toString());
                    }
                    // Reset
                    step = 0;
                    timeLine = null;
                    textBuilder.setLength(0);
                    continue;
                }

                // SRT Logic
                // Step 0: Expect Index
                if (step == 0) {
                    if (line.matches("\\d+")) {
                        try {
                            index = Integer.parseInt(line);
                            step = 1;
                        } catch (NumberFormatException e) {
                            // Maybe not an index but garbage line?
                            // Ignore
                        }
                    }
                    // Note: broken SRTs might skip index lines.
                    // If we see timestamps in step 0, handle it.
                    if (line.contains("-->")) {
                        timeLine = line;
                        step = 2;
                    }
                }
                // Step 1: Expect Time
                else if (step == 1) {
                    if (line.contains("-->")) {
                        timeLine = line;
                        step = 2;
                    } else {
                        // Unexpected? Reset or treat as text?
                        // Treat as text if we missed strict structure
                        textBuilder.append(line).append(" ");
                    }
                }
                // Step 2: Expect Text
                else if (step == 2) {
                    textBuilder.append(line).append(" ");
                }
            }

            // Pending buffer
            if (timeLine != null && textBuilder.length() > 0) {
                addSubItem(subtitles, index, timeLine, textBuilder.toString());
            }

        } catch (Exception e) {
            log.error("Error parsing SRT file", e);
        }

        log.info("Parsed {} subtitles from {}", subtitles.size(), file.getName());
        return subtitles;
    }

    private void addSubItem(List<VideoInfoVO.SubtitleItemVO> list, int index, String timeLine, String text) {
        try {
            String[] parts = timeLine.split("-->");
            if (parts.length < 2)
                return;

            long start = parseTime(parts[0].trim());
            long end = parseTime(parts[1].trim());

            String cleanText = text.replaceAll("<[^>]*>", "").trim();
            if (!cleanText.isEmpty()) {
                list.add(new VideoInfoVO.SubtitleItemVO(index, start, end, cleanText));
            }
        } catch (Exception e) {
            // log.warn("Bad subtitle item: " + index);
        }
    }

    private long parseTime(String time) {
        // SRT uses: 00:00:00,000 (Comma)
        time = time.replace(",", "."); // Standardize to dot for easier parsing if reusing logic, or parse directly

        try {
            String[] parts = time.split(":");
            long ms = 0;
            if (parts.length == 3) {
                ms += Long.parseLong(parts[0]) * 3600000;
                ms += Long.parseLong(parts[1]) * 60000;
                String[] seconds = parts[2].split("\\.");
                ms += Long.parseLong(seconds[0]) * 1000;
                if (seconds.length > 1) {
                    ms += Long.parseLong(seconds[1]);
                }
            }
            return ms;
        } catch (Exception e) {
            return 0;
        }
    }
}
