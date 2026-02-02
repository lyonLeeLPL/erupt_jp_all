package com.example.demo.utils;

import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class IpUtils {

    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    public String getRegion(String ip) {
        if (ip == null)
            return "Unknown";

        // 尝试使用 ip-api.com (强烈推荐的第三方API)
        try {
            String urlStr = "http://ip-api.com/json/" + ip + "?lang=zh-CN";
            java.net.URL url = java.net.URI.create(urlStr).toURL();
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000); // 3秒超时
            conn.setReadTimeout(3000);

            if (conn.getResponseCode() == 200) {
                java.io.BufferedReader in = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream(), "UTF-8"));
                String line;
                StringBuilder result = new StringBuilder();
                while ((line = in.readLine()) != null) {
                    result.append(line);
                }
                in.close();

                // 解析 JSON
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(result.toString());

                if ("success".equals(node.path("status").asText())) {
                    String country = node.path("country").asText();
                    String regionName = node.path("regionName").asText();
                    String city = node.path("city").asText();
                    String isp = node.path("isp").asText();
                    return String.format("%s|%s|%s %s", country, regionName, city, isp);
                }
            }
        } catch (Exception e) {
            // API 失败，降级处理 (记录日志或忽略)
            System.err.println("ip-api.com fetch failed: " + e.getMessage());
        }

        // 降级：使用 Erupt 内置的 IpUtil
        try {
            return xyz.erupt.upms.util.IpUtil.getCityInfo(ip);
        } catch (Exception e) {
            e.printStackTrace();
            return "Error";
        }
    }
}
