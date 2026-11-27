package kr.ac.yumin.playlist.model;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public record OnlineVideo(
        long songId,
        String videoId,
        String title,
        String channel,
        String kind,
        String query,
        String status) {
    public boolean hasVideo() {
        return videoId != null && videoId.matches("[A-Za-z0-9_-]{11}");
    }

    public String url() {
        return hasVideo()
                ? "https://www.youtube.com/watch?v=" + videoId + "&autoplay=1&hl=ko"
                : searchUrl();
    }

    public String searchUrl() {
        return "https://www.youtube.com/results?search_query="
                + URLEncoder.encode(query, StandardCharsets.UTF_8)
                + "&hl=ko";
    }

    public String kindLabel() {
        return switch (kind) {
            case "MV" -> "공식 뮤직비디오";
            case "AUDIO" -> "공식 음원 영상";
            case "LIVE" -> "공식 공연 영상";
            default -> "영상 검색";
        };
    }
}
