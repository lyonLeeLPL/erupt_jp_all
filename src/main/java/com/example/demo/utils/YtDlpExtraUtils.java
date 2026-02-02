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

    public String downloadSubtitle(String url) {
        // Create a temp directory for subtitles
        String tempDir = System.getProperty("java.io.tmpdir");
        String outputTemplate = tempDir + "/%(id)s.%(ext)s";

        // yt-dlp --skip-download --write-auto-sub --sub-lang en --sub-format vtt -o
        // [template] [url]
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "yt-dlp",
                    "--skip-download",
                    "--write-auto-sub",
                    "--sub-lang", "en",
                    "--sub-format", "vtt",
                    "-o", outputTemplate,
                    url);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Read output to avoid blocking (even if we don't use it)
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                while (reader.readLine() != null) {
                }
            }

            if (!process.waitFor(60, TimeUnit.SECONDS)) {
                process.destroy();
                throw new RuntimeException("Timeout downloading subtitle");
            }

            // Find the file. yt-dlp might append .en.vtt or just .vtt
            // Note: obtaining the Video ID here again or assuming it from URL is tricky if
            // we don't know it.
            // But we can parse the video info first to get ID.
            // For safety, let's use the ID we *should* have.
            // Actually, it's safer to pass VideoID to this method, but the current
            // interface uses URL.
            // Let's deduce the filename.

            // Quick hack: The file should be there.
            // We need to know the ID to find the file.
            // Let's update the signature to accept videoId if possible, or extract it.

        } catch (Exception e) {
            throw new RuntimeException("Download failed: " + e.getMessage());
        }
        return null; // Placeholder until we fix the signature
    }

    public String downloadSubtitleContent(String videoId, String url) throws Exception {
        String tempDir = System.getProperty("java.io.tmpdir");
        // CLEANUP: Clean old files if needed

        // Format: [id].en.vtt (usually yt-dlp adds lang code)
        String filename = videoId;
        String outputTemplate = tempDir + (tempDir.endsWith("/") ? "" : "/") + filename + ".%(ext)s";

        ProcessBuilder pb = new ProcessBuilder(
                "yt-dlp",
                "--skip-download",
                "--write-auto-sub",
                "--sub-lang", "en",
                "--sub-format", "vtt",
                "-o", outputTemplate,
                url);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            while (reader.readLine() != null) {
            }
        }
        process.waitFor(60, TimeUnit.SECONDS);

        // Helper to find the file
        java.io.File vttFile = new java.io.File(tempDir, filename + ".en.vtt");
        if (!vttFile.exists()) {
            // Try without .en? or just .vtt
            vttFile = new java.io.File(tempDir, filename + ".vtt");
        }

        if (vttFile.exists()) {
            String content = java.nio.file.Files.readString(vttFile.toPath());
            vttFile.delete(); // Cleanup
            return content;
        }

        return null;
    }
}
