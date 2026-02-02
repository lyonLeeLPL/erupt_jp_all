package com.example.demo.model.subtitle;

import java.util.List;

public class VideoInfoVO {
    private String videoId;
    private String title;
    private long duration; // seconds
    private List<SubtitleItemVO> subtitles;

    // Getters and Setters
    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public List<SubtitleItemVO> getSubtitles() {
        return subtitles;
    }

    public void setSubtitles(List<SubtitleItemVO> subtitles) {
        this.subtitles = subtitles;
    }

    public static class SubtitleItemVO {
        private int index;
        private long startTime; // milliseconds
        private long endTime; // milliseconds
        private String original;
        private String translation;

        public SubtitleItemVO(int index, long startTime, long endTime, String original) {
            this.index = index;
            this.startTime = startTime;
            this.endTime = endTime;
            this.original = original;
        }

        // Getters and Setters
        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public long getStartTime() {
            return startTime;
        }

        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }

        public long getEndTime() {
            return endTime;
        }

        public void setEndTime(long endTime) {
            this.endTime = endTime;
        }

        public String getOriginal() {
            return original;
        }

        public void setOriginal(String original) {
            this.original = original;
        }

        public String getTranslation() {
            return translation;
        }

        public void setTranslation(String translation) {
            this.translation = translation;
        }
    }
}
