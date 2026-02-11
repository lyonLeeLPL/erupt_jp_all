package com.example.demo.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

@Component
public class YtDlpExtraUtils {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${erupt.youtube-path}")
    private String youtubePath;

    public JsonNode getVideoInfo(String url) throws Exception {

        // Command: yt-dlp --dump-json --skip-download [url]
        Process process = getProcess(new ProcessBuilder("C:\\666_sdk\\yt-dlp.exe", "--dump-json", "--skip-download", url));

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
        String tempDir = youtubePath + File.separator + "sublingo";
        File dir = new File(tempDir);
        if (!dir.exists())
            dir.mkdirs();

        // We force vtt
        String outputTemplate = tempDir + File.separator + videoId + ".%(ext)s";

        // Check if file already exists
        File expectedFile = new File(tempDir, videoId + ".ja.srt");
        if (expectedFile.exists()) {
            return expectedFile;
        }

        // Cmd: yt-dlp --cookies cookies.txt --js-runtimes node --skip-download
        // Cmd: yt-dlp --cookies cookies.txt --js-runtimes node --skip-download
        Process process = getProcess(new ProcessBuilder(
                "C:\\666_sdk\\yt-dlp.exe",
                "--cookies", "C:\\666_sdk\\cookies.txt", // User provided
                "--js-runtimes", "node", // User provided
                "--skip-download",
                "--write-sub",
                "--write-auto-sub",
                "--sub-format", "srt", // Force convert to SRT
                "--sub-lang", "zh-Hans,ja", // Try English, fallback to Japanese
                "-o", outputTemplate,
                url));

        // Consume output to prevent blocking
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            // System.out.println(line); // debug
        }

        process.waitFor(60, TimeUnit.SECONDS);

        // Find the file. yt-dlp appends language code, e.g. videoID.en.srt
        File srtFile = new File(tempDir, videoId + ".ja.srt");
        if (srtFile.exists())
            return srtFile;

        // Try other variations usually produced by yt-dlp
        // Check for ANY .srt file starting with videoId
        File[] validFiles = dir.listFiles((d, name) -> name.startsWith(videoId) && name.endsWith(".srt"));
        if (validFiles != null && validFiles.length > 0) {
            return validFiles[0];
        }

        return null;
    }

    @NotNull
    private static Process getProcess(ProcessBuilder outputTemplate) throws IOException {
        ProcessBuilder pb = outputTemplate;

        pb.redirectErrorStream(true);
        Process process = pb.start();
        return process;
    }
}
