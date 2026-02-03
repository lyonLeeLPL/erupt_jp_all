package com.example.demo.utils;

import com.example.demo.model.subtitle.VideoInfoVO;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class VttParser {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(VttParser.class);
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("(\\d{2}:)?(\\d{2}:)?\\d{2}\\.\\d{3}");

    public List<VideoInfoVO.SubtitleItemVO> parse(File file) {
        List<VideoInfoVO.SubtitleItemVO> subtitles = new ArrayList<>();
        if (file == null || !file.exists())
            return subtitles;

        try (BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(file),
                java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            String timeLine = null;
            StringBuilder textBuilder = new StringBuilder();
            int index = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip header 'WEBVTT' or empty lines at start
                if (line.isEmpty()) {
                    // Empty line usually separates segments.
                    // However, we only flush if we have pending data to avoid flushing on initial
                    // empty lines.
                    if (timeLine != null && textBuilder.length() > 0) {
                        addSubItem(subtitles, ++index, timeLine, textBuilder.toString());
                        textBuilder.setLength(0);
                        timeLine = null; // Reset state
                    }
                    continue;
                }

                // Check for timestamp line: "00:00:01.000 --> 00:00:04.000"
                if (line.contains("-->")) {
                    // Safety flush: if we stumbled upon a new timestamp but state wasn't cleared
                    // (e.g. strict VTT compliance issue)
                    if (timeLine != null && textBuilder.length() > 0) {
                        addSubItem(subtitles, ++index, timeLine, textBuilder.toString());
                        textBuilder.setLength(0);
                    }
                    timeLine = line;
                } else if (timeLine != null) {
                    // This is text content
                    // Check if it looks like an index number just in case?
                    // Usually handled by empty line check, but good for robustness.
                    // But some text might be just numbers. Let's trust empty line logic first.
                    if (textBuilder.length() > 0)
                        textBuilder.append(" ");
                    textBuilder.append(line);
                }
            }

        } catch (Exception e) {
            log.error("Error parsing VTT file", e);
            e.printStackTrace();
        }

        log.info("Parsed {} subtitles from {}", subtitles.size(), file.getName());
        return subtitles;
    }

    private void addSubItem(List<VideoInfoVO.SubtitleItemVO> list, int index, String timeLine, String text) {
        // Parse Timestamps
        String[] parts = timeLine.split("-->");
        if (parts.length < 2)
            return;

        long start = parseTime(parts[0].trim());
        long end = parseTime(parts[1].trim());

        // Clean text (remove tags like <c.color> or &nbsp;)
        String cleanText = text.replaceAll("<[^>]*>", "").trim();
        if (!cleanText.isEmpty()) {
            list.add(new VideoInfoVO.SubtitleItemVO(index, start, end, cleanText));
        }
    }

    private long parseTime(String time) {
        // Formats: MM:SS.mmm or HH:MM:SS.mmm
        // VTT lines might be: "00:00:04.000 align:start position:0%"
        // We need to strip extra attributes.
        String timestamp = time.trim().split("\\s+")[0];

        try {
            String[] parts = timestamp.split(":");
            long ms = 0;
            if (parts.length == 3) {
                // HH:MM:SS.mmm
                ms += Long.parseLong(parts[0]) * 3600000;
                ms += Long.parseLong(parts[1]) * 60000;
                ms += (long) (Double.parseDouble(parts[2]) * 1000);
            } else if (parts.length == 2) {
                // MM:SS.mmm
                ms += Long.parseLong(parts[0]) * 60000;
                ms += (long) (Double.parseDouble(parts[1]) * 1000);
            }
            return ms;
        } catch (Exception e) {
            // System.err.println("Failed to parse time: " + timestamp);
            return 0;
        }
    }
}
