package com.example.demo.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
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

    public java.io.File downloadSubtitle(String url, JsonNode meta) throws Exception {
        // 1. Get ID to construct filename
        String videoId = meta.get("id").asText();

        // Output format: /tmp/sublingo/videoID
        String tempDir = System.getProperty("java.io.tmpdir") + File.separator + "sublingo";
        File dir = new File(tempDir);
        if (!dir.exists())
            dir.mkdirs();

        // We force vtt
        String outputTemplate = tempDir + File.separator + videoId + ".%(ext)s";

        // Check if file already exists
        File expectedFile = new File(tempDir, videoId + ".ja.vtt");
        if (expectedFile.exists()) {
            return expectedFile;
        }

        // Cmd: yt-dlp --cookies cookies.txt --js-runtimes node --skip-download
        ProcessBuilder pb = new ProcessBuilder(
                "yt-dlp",
                "--cookies", "C:\\666_sdk\\cookies.txt", // User provided
                "--js-runtimes", "node", // User provided
                "--skip-download",
                "--write-sub",
                "--write-auto-sub",
                "--sub-lang", "zh-Hans,ja", // Try English, fallback to Japanese
                "--sub-format", "vtt",
                "-o", outputTemplate,
                url);

        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Consume output to prevent blocking
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            // System.out.println(line); // debug
        }

        process.waitFor(60, TimeUnit.SECONDS);

        // Find the file. yt-dlp appends language code, e.g. videoID.en.vtt
        File vttFile = new File(tempDir, videoId + ".ja.vtt");
        if (vttFile.exists())
            return vttFile;

        // Try other variations usually produced by yt-dlp
        // Sometimes it's videoID.vtt if language is implicit? strictly it appends lang.
        // Check for ANY .vtt file starting with videoId
        File[] validFiles = dir.listFiles((d, name) -> name.startsWith(videoId) && name.endsWith(".vtt"));
        if (validFiles != null && validFiles.length > 0) {
            return validFiles[0];
        }

        return null;
    }
}
