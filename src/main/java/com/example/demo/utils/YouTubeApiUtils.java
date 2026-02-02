package com.example.demo.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

@Component
public class YouTubeApiUtils {

    private final String API_KEY = "AIzaSyDvlUGRP4aLK8LeQFAA1MeAL23oqVkCVRA";
    private final String BASE_URL = "https://www.googleapis.com/youtube/v3";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonNode getVideoDetails(String videoId) throws Exception {
        String url = BASE_URL + "/videos?part=snippet,contentDetails&id=" + videoId + "&key=" + API_KEY;
        String response = restTemplate.getForObject(url, String.class);
        JsonNode root = objectMapper.readTree(response);
        if (root.has("items") && root.get("items").size() > 0) {
            return root.get("items").get(0);
        }
        return null;
    }

    public String getCaptionId(String videoId, String lang) throws Exception {
        String url = BASE_URL + "/captions?part=snippet&videoId=" + videoId + "&key=" + API_KEY;
        String response = restTemplate.getForObject(url, String.class);
        JsonNode root = objectMapper.readTree(response);

        if (root.has("items")) {
            for (JsonNode item : root.get("items")) {
                JsonNode snippet = item.get("snippet");
                String language = snippet.get("language").asText();
                // Prefer standard track if available, but take any matching language
                if (language.equals(lang)) {
                    return item.get("id").asText();
                }
            }
        }
        return null;
    }

    public String downloadCaption(String captionId) {
        // Note: Downloading captions via API usually requires OAuth 2.0 for owned
        // videos.
        // For public videos, this might return 403 Forbidden.
        // However, we will try the official endpoint with tfmt=vtt.
        String url = BASE_URL + "/captions/" + captionId + "?tfmt=vtt&key=" + API_KEY;

        HttpHeaders headers = new HttpHeaders();
        // Sometimes OAuth is required, sometimes not needed for ASR.
        // If this fails, we might need a different strategy (like tfmt=srv3 or
        // scraping),
        // but let's try the standard API first as requested.

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        return response.getBody();
    }
}
