package kr.ac.yumin.playlist.service;

import com.google.gson.*;

import kr.ac.yumin.playlist.config.Database;
import kr.ac.yumin.playlist.dao.Jdbc;
import kr.ac.yumin.playlist.model.OnlineVideo;

import java.net.*;
import java.net.http.*;
import java.sql.*;
import java.time.Duration;
import java.util.*;

public final class OnlineVideoService {
    public Map<Long, String> states() throws SQLException {
        try (Connection c = Database.open()) {
            Map<Long, String> states = new HashMap<>();
            for (var row :
                    Jdbc.list(
                            c,
                            "SELECT song_id,match_status FROM SONG_VIDEO",
                            r -> Map.entry(r.getLong(1), r.getString(2))))
                states.put(row.getKey(), row.getValue());
            return Map.copyOf(states);
        }
    }

    public OnlineVideo resolve(long songId) throws SQLException {
        try (Connection c = Database.open()) {
            return Jdbc.list(
                            c,
                            "SELECT"
                                + " song_id,youtube_video_id,youtube_title,channel_name,video_kind,search_query,match_status"
                                + " FROM SONG_VIDEO WHERE song_id=?",
                            r ->
                                    new OnlineVideo(
                                            r.getLong(1),
                                            r.getString(2),
                                            r.getString(3),
                                            r.getString(4),
                                            r.getString(5),
                                            r.getString(6),
                                            r.getString(7)),
                            songId)
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new AppException("이 곡의 온라인 재생 정보가 없습니다. 보관함을 다시 불러와 주세요."));
        }
    }

    public record Metadata(String id, String title, String author) {}

    public Metadata metadata(String id) throws Exception {
        if (id == null || !id.matches("[A-Za-z0-9_-]{11}"))
            throw new AppException("유튜브 영상 번호가 올바르지 않습니다.");
        URI uri =
                URI.create(
                        "https://www.youtube.com/oembed?url=https%3A%2F%2Fwww.youtube.com%2Fwatch%3Fv%3D"
                                + id
                                + "&format=json");
        {
            HttpClient client =
                    HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
            var response =
                    client.send(
                            HttpRequest.newBuilder(uri)
                                    .timeout(Duration.ofSeconds(12))
                                    .GET()
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200)
                throw new AppException("공개 영상 정보를 확인할 수 없습니다. 앱 안에서 다른 공식 영상을 선택해 주세요.");
            JsonObject object = JsonParser.parseString(response.body()).getAsJsonObject();
            return new Metadata(
                    id, object.get("title").getAsString(), object.get("author_name").getAsString());
        }
    }

    public void save(long songId, Metadata metadata, String kind) throws SQLException {
        if (!Set.of("MV", "AUDIO", "LIVE").contains(kind)
                || !metadata.id().matches("[A-Za-z0-9_-]{11}"))
            throw new AppException("영상 정보가 올바르지 않습니다.");
        try (Connection c = Database.open()) {
            if (Jdbc.update(
                            c,
                            "UPDATE SONG_VIDEO SET"
                                + " youtube_video_id=?,youtube_title=?,channel_name=?,video_kind=?,match_status='METADATA',checked_at=SYSTIMESTAMP"
                                + " WHERE song_id=?",
                            metadata.id(),
                            metadata.title(),
                            metadata.author(),
                            kind,
                            songId)
                    != 1) throw new AppException("등록된 곡을 찾을 수 없습니다.");
        }
    }

    public void confirmed(long songId, String id) throws SQLException {
        try (Connection c = Database.open()) {
            Jdbc.update(
                    c,
                    "UPDATE SONG_VIDEO SET match_status='PLAYABLE',checked_at=SYSTIMESTAMP WHERE"
                            + " song_id=? AND youtube_video_id=?",
                    songId,
                    id);
        }
    }

    public static String videoId(String url) {
        try {
            URI uri = URI.create(url);
            if (!"https".equals(uri.getScheme())
                    || !Set.of("www.youtube.com", "youtube.com", "m.youtube.com")
                            .contains(uri.getHost())
                    || !"/watch".equals(uri.getPath())) return null;
            for (String part : Optional.ofNullable(uri.getRawQuery()).orElse("").split("&"))
                if (part.matches("v=[A-Za-z0-9_-]{11}")) return part.substring(2);
        } catch (Exception ignored) {
        }
        return null;
    }
}
