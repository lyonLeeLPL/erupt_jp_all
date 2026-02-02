package com.example.demo.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

@Component
public class YtDlpExtraUtils {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonNode getVideoInfo(String url) throws Exception {
        // Command: yt-dlp --dump-json --skip-download [url]
        ProcessBuilder pb = new ProcessBuilder("yt-dlp", "--dump-json", "--skip-download", url);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder jsonOutput = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Filter out logs/warnings. Real JSON output starts with "{"
                if (line.trim().startsWith("{")) {
                    jsonOutput.append(line);
                }
            }
        }

        String result = jsonOutput.toString();
        if (result.isEmpty()) {
            throw new RuntimeException("No JSON output from yt-dlp");
        }

        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroy();
            throw new RuntimeException("Timeout executing yt-dlp");
        }

        if (process.exitValue() != 0) {
            throw new RuntimeException("yt-dlp failed with exit code " + process.exitValue());
        }

        return objectMapper.readTree(result);
    }
}
