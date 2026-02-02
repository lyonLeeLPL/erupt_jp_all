package com.example.demo.utils;

import com.example.demo.model.subtitle.VideoInfoVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SubtitleParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<VideoInfoVO.SubtitleItemVO> parseVtt(String vttContent) {
        List<VideoInfoVO.SubtitleItemVO> result = new ArrayList<>();
        String[] lines = vttContent.split("\n");
        int index = 0;

        long startTime = 0;
        long endTime = 0;
        StringBuilder text = new StringBuilder();

        boolean readingText = false;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("WEBVTT") || line.startsWith("Kind:")
                    || line.startsWith("Language:")) {
                if (readingText && text.length() > 0) {
                    result.add(new VideoInfoVO.SubtitleItemVO(++index, startTime, endTime, text.toString().trim()));
                    text.setLength(0);
                    readingText = false;
                }
                continue;
            }

            if (line.contains("-->")) {
                // Save previous if exists
                if (readingText && text.length() > 0) {
                    result.add(new VideoInfoVO.SubtitleItemVO(++index, startTime, endTime, text.toString().trim()));
                    text.setLength(0);
                }

                // Parse Timestamp: 00:00:05.500 --> 00:00:09.300
                try {
                    String[] times = line.split("-->");
                    startTime = parseVttTime(times[0].trim());
                    endTime = parseVttTime(times[1].trim());
                    readingText = true;
                } catch (Exception e) {
                    // Invalid time line
                }
            } else if (readingText) {
                text.append(line).append(" ");
            }
        }
        // Add last entry
        if (readingText && text.length() > 0) {
            result.add(new VideoInfoVO.SubtitleItemVO(++index, startTime, endTime, text.toString().trim()));
        }

        return result;
    }

    private long parseVttTime(String timeStr) {
        // Format: HH:mm:ss.SSS or mm:ss.SSS
        String[] parts = timeStr.split(":");
        long ms = 0;
        try {
            if (parts.length == 3) {
                ms += Long.parseLong(parts[0]) * 3600000;
                ms += Long.parseLong(parts[1]) * 60000;
                String[] seconds = parts[2].split("\\.");
                ms += Long.parseLong(seconds[0]) * 1000;
                if (seconds.length > 1)
                    ms += Long.parseLong(seconds[1]);
            } else if (parts.length == 2) {
                ms += Long.parseLong(parts[0]) * 60000;
                String[] seconds = parts[1].split("\\.");
                ms += Long.parseLong(seconds[0]) * 1000;
                if (seconds.length > 1)
                    ms += Long.parseLong(seconds[1]);
            }
        } catch (NumberFormatException e) {
            return 0;
        }
        return ms;
    }

    /**
     * Parses YouTube 'json3' format (timed text).
     */
    public List<VideoInfoVO.SubtitleItemVO> parseJson3(String jsonContent) {
        List<VideoInfoVO.SubtitleItemVO> result = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(jsonContent);
            if (!root.has("events"))
                return result;

            JsonNode events = root.get("events");
            int index = 0;

            if (events.isArray()) {
                for (JsonNode event : events) {
                    if (!event.has("segs") || !event.has("tStartMs"))
                        continue;

                    // Start Time
                    long startTime = event.get("tStartMs").asLong();
                    // Duration (optional)
                    long duration = event.has("dDurationMs") ? event.get("dDurationMs").asLong() : 0;
                    long endTime = startTime + duration;

                    // Segments (Text)
                    StringBuilder textBuilder = new StringBuilder();
                    JsonNode segs = event.get("segs");
                    if (segs.isArray()) {
                        for (JsonNode seg : segs) {
                            if (seg.has("utf8")) {
                                textBuilder.append(seg.get("utf8").asText());
                            }
                        }
                    }

                    String text = textBuilder.toString().trim();
                    // Skip empty lines or newlines
                    if (text.isEmpty() || text.equals("\n"))
                        continue;

                    result.add(new VideoInfoVO.SubtitleItemVO(++index, startTime, endTime, text));
                }
            }
        } catch (Exception e) {
            e.printStackTrace(); // Log error
        }

        return result;
    }
}
