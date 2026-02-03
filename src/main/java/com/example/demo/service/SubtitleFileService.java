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
 * - Scanning directory
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
     * Scan directory to find the best matching source and target VTT files for a
     * videoId.
     */
    public SubtitleFilesDTO findSubtitleFiles(String videoId) {
        File dir = getSubtitleDir();
        if (!dir.exists()) {
            return new SubtitleFilesDTO(null, null);
        }

        File[] files = dir.listFiles((d, name) -> name.startsWith(videoId) && name.endsWith(".vtt"));

        File bestSource = null;
        File bestTarget = null;

        if (files != null) {
            for (File f : files) {
                String name = f.getName();

                // Extract lang code: videoId.LANG.vtt
                // Be careful with substrings
                if (name.length() <= videoId.length() + 4)
                    continue; // safety for ".vtt"

                String suffix = name.substring(videoId.length());
                if (suffix.startsWith("."))
                    suffix = suffix.substring(1);
                String lang = suffix.replace(".vtt", "").toLowerCase();

                logger.info("Found VTT: " + name + " (Lang: " + lang + ")");

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
     * Write string content to a new VTT file
     */
    public void saveMergedVtt(String videoId, String content) {
        File dir = getSubtitleDir();
        File mergedFile = new File(dir, videoId + ".merged.vtt");
        try {
            Files.writeString(mergedFile.toPath(), content, StandardCharsets.UTF_8);
            logger.info("Saved Merged VTT to: " + mergedFile.getAbsolutePath());
        } catch (IOException e) {
            logger.warning("Failed to save merged VTT: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
