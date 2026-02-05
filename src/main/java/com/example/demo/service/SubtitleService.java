package com.example.demo.service;

import com.example.demo.model.subtitle.VideoInfoVO;
import com.example.demo.utils.YtDlpExtraUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Main Service for Subtitle Operations.
 * Orchestrates the flow of fetching info, downloading, processing, and merging
 * subtitles.
 */
@Service
public class SubtitleService {

    private static final Logger logger = Logger.getLogger(SubtitleService.class.getName());

    @Autowired
    private YtDlpExtraUtils ytDlpUtils;

    @Autowired
    private SubtitleFileService subtitleFileService;

    @Autowired
    private SubtitleProcessingService processingService;

    public VideoInfoVO processVideo(String url) {
        VideoInfoVO vo = new VideoInfoVO();
        try {
            // 1. Fetch Metadata
            JsonNode meta = ytDlpUtils.getVideoInfo(url);
            String videoId = meta.get("id").asText();

            vo.setVideoId(videoId);
            vo.setTitle(meta.get("title").asText());
            vo.setDuration(meta.get("duration").asLong());

            // 2. Trigger Download (Creates files in temp dir)
            try {
                ytDlpUtils.downloadSubtitle(url, meta);
            } catch (Exception e) {
                logger.warning("Download warning: " + e.getMessage());
            }

            // 3. Find Subtitle Files on Disk
            SubtitleFileService.SubtitleFilesDTO files = subtitleFileService.findSubtitleFiles(videoId);

            // 4. Parse & Process
            List<VideoInfoVO.SubtitleItemVO> sourceList = processingService.parseAndFilter(files.getSource());
            List<VideoInfoVO.SubtitleItemVO> targetList = processingService.parseAndFilter(files.getTarget());

            if (files.getSource() != null)
                logger.info("Using Source: " + files.getSource().getName());
            if (files.getTarget() != null)
                logger.info("Using Target: " + files.getTarget().getName());

            // 5. Merge
            processingService.mergeSubtitles(sourceList, targetList);

            // 6. Generate & Save Result
            if (!sourceList.isEmpty()) {
                // OPTIMIZATION: Extend end times to fill gaps
                processingService.fillTimelineGaps(sourceList);

                String mergedContent = processingService.generateSrtContent(sourceList);
                subtitleFileService.saveMergedSrt(videoId, mergedContent);
                vo.setRawVtt(mergedContent); // Actually SRT now
                vo.setSubtitles(sourceList); // JSON list structure is same
            } else {
                vo.setSubtitles(new ArrayList<>());
            }

            vo.setTranslationVtt(null);

        } catch (Exception e) {
            logger.severe("Error in SubtitleService: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error processing video: " + e.getMessage());
        }
        return vo;
    }
}
