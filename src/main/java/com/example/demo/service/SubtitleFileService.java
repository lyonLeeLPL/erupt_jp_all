package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.logging.Logger;

/**
 * Service for File I/O related to Subtitles
 * - Scanning directory for SRT files
 * - Identification of Source/Target files
 * - Saving generated files
 */
@Service
public class SubtitleFileService {

    private static final Logger logger = Logger.getLogger(SubtitleFileService.class.getName());

    @Value("${erupt.youtube-path}")
    private String youtubePath;

    public static class SubtitleFilesDTO {
        private File source;
        private File target;

        public SubtitleFilesDTO(File source, File target) {
            this.source = source;
            this.target = target;
        }

        public File getSource() {
            return source;
        }

        public File getTarget() {
            return target;
        }
    }

    public File getSubtitleDir() {
        String tempDir = youtubePath + File.separator + "sublingo";
        File dir = new File(tempDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * Scan directory to find the best matching source and target SRT files for a
     * videoId.
     */
    public SubtitleFilesDTO findSubtitleFiles(String videoId) {
        File dir = getSubtitleDir();
        if (!dir.exists()) {
            return new SubtitleFilesDTO(null, null);
        }

        // Look for .srt files
        File[] files = dir.listFiles((d, name) -> name.startsWith(videoId) && name.endsWith(".srt"));

        File bestSource = null;
        File bestTarget = null;

        if (files != null) {
            for (File f : files) {
                String name = f.getName();

                // Extract lang code: videoId.LANG.srt
                // Be careful with substrings
                if (name.length() <= videoId.length() + 4)
                    continue; // safety for ".srt"

                String suffix = name.substring(videoId.length());
                if (suffix.startsWith("."))
                    suffix = suffix.substring(1);
                String lang = suffix.replace(".srt", "").toLowerCase();

                logger.info("Found SRT: " + name + " (Lang: " + lang + ")");

                // Strategy Source: Japanese (ja) > English (en)
                if (lang.startsWith("ja")) {
                    bestSource = f;
                } else if (lang.startsWith("en") && (bestSource == null || !bestSource.getName().contains(".ja"))) {
                    if (bestSource == null)
                        bestSource = f;
                }

                // Strategy Target: Chinese (zh)
                if (lang.startsWith("zh")) {
                    bestTarget = f;
                }
            }
        }

        return new SubtitleFilesDTO(bestSource, bestTarget);
    }

    /**
     * Retrieve subtitle file by type
     * 
     * @param type "source" (JA/Original) or "merged" (Processed)
     */
    public File getSubtitleFile(String videoId, String type) {
        SubtitleFilesDTO files = findSubtitleFiles(videoId);

        if ("source".equalsIgnoreCase(type)) {
            return files.getSource();
        } else if ("merged".equalsIgnoreCase(type)) {
            // First check for the .merged.srt we generated
            File dir = getSubtitleDir();
            File merged = new File(dir, videoId + ".merged.srt");
            if (merged.exists())
                return merged;

            // Fallback?
            return null;
        }
        return null;
    }

    /**
     * Write string content to a new SRT file
     */
    public void saveMergedSrt(String videoId, String content) {
        File dir = getSubtitleDir();
        File mergedFile = new File(dir, videoId + ".merged.srt");
        try {
            Files.writeString(mergedFile.toPath(), content, StandardCharsets.UTF_8);
            logger.info("Saved Merged SRT to: " + mergedFile.getAbsolutePath());
        } catch (IOException e) {
            logger.warning("Failed to save merged SRT: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
