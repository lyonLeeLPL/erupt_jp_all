package com.example.demo.utils;

import com.example.demo.model.subtitle.VideoInfoVO;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class VttParser {

    // Regex for: 00:00:01.000 --> 00:00:04.000
    private static final Pattern TIME_PATTERN = Pattern
            .compile("((?:\\d{2}:)?\\d{2}:\\d{2}\\.\\d{3})\\s-->\\s((?:\\d{2}:)?\\d{2}:\\d{2}\\.\\d{3})");

    public List<VideoInfoVO.SubtitleItemVO> parse(String vttContent) {
        List<VideoInfoVO.SubtitleItemVO> items = new ArrayList<>();
        if (vttContent == null || vttContent.isEmpty())
            return items;

        try (BufferedReader reader = new BufferedReader(new StringReader(vttContent))) {
            String line;
            int index = 0;
            long currentStart = 0;
            long currentEnd = 0;
            StringBuilder textBuffer = new StringBuilder();
            boolean inCue = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip header or empty lines between blocks
                if (line.isEmpty() || line.startsWith("WEBVTT") || line.startsWith("NOTE")) {
                    // If we were processing a cue and hit an empty line, save it
                    if (inCue && textBuffer.length() > 0) {
                        items.add(new VideoInfoVO.SubtitleItemVO(++index, currentStart, currentEnd,
                                textBuffer.toString().trim()));
                        textBuffer.setLength(0);
                        inCue = false;
                    }
                    continue;
                }

                Matcher matcher = TIME_PATTERN.matcher(line);
                if (matcher.find()) {
                    // If previously in a cue but didn't hit empty line (edge case), save it
                    if (inCue && textBuffer.length() > 0) {
                        items.add(new VideoInfoVO.SubtitleItemVO(++index, currentStart, currentEnd,
                                textBuffer.toString().trim()));
                        textBuffer.setLength(0);
                    }

                    // Start new cue
                    currentStart = parseTime(matcher.group(1));
                    currentEnd = parseTime(matcher.group(2));
                    inCue = true;
                } else if (inCue) {
                    // Content text
                    if (textBuffer.length() > 0)
                        textBuffer.append(" ");
                    textBuffer.append(line);
                }
            }

            // Final flush
            if (inCue && textBuffer.length() > 0) {
                items.add(new VideoInfoVO.SubtitleItemVO(++index, currentStart, currentEnd,
                        textBuffer.toString().trim()));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return items;
    }

    private long parseTime(String timeStr) {
        // Formats: MM:SS.mmm or HH:MM:SS.mmm
        String[] parts = timeStr.split(":");
        long ms = 0;

        if (parts.length == 3) {
            // HH:MM:SS.mmm
            ms += Long.parseLong(parts[0]) * 3600000;
            ms += Long.parseLong(parts[1]) * 60000;
            String[] secParts = parts[2].split("\\.");
            ms += Long.parseLong(secParts[0]) * 1000;
            ms += Long.parseLong(secParts[1]);
        } else if (parts.length == 2) {
            // MM:SS.mmm
            ms += Long.parseLong(parts[0]) * 60000;
            String[] secParts = parts[1].split("\\.");
            ms += Long.parseLong(secParts[0]) * 1000;
            ms += Long.parseLong(secParts[1]);
        }
        return ms;
    }
}
