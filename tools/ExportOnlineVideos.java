import com.google.gson.GsonBuilder;

import kr.ac.yumin.playlist.config.Database;
import kr.ac.yumin.playlist.dao.Jdbc;

import java.nio.file.*;
import java.util.*;

/** Public catalog facts only. No members, payment history, browser profile or media data. */
public class ExportOnlineVideos {
    record Video(
            long songId,
            String songTitle,
            String artists,
            String videoId,
            String title,
            String channel,
            String kind,
            String searchQuery,
            String status,
            String checkedAt) {}

    private static String q(String s) {
        return s == null ? "NULL" : "'" + s.replace("'", "''") + "'";
    }

    public static void main(String[] args) throws Exception {
        java.util.List<Video> rows;
        try (var c = Database.open()) {
            rows =
                    Jdbc.list(
                            c,
                            "SELECT s.song_id,s.title,(SELECT LISTAGG(a.artist_name,', ') WITHIN"
                                + " GROUP(ORDER BY a.artist_id) FROM SONG_ARTIST sa JOIN ARTIST a"
                                + " ON a.artist_id=sa.artist_id WHERE"
                                + " sa.song_id=s.song_id),v.youtube_video_id,v.youtube_title,v.channel_name,v.video_kind,v.search_query,v.match_status,TO_CHAR(v.checked_at,'YYYY-MM-DD"
                                + " HH24:MI:SS') FROM MUSIC_CATALOG cat JOIN SONG s ON"
                                + " s.song_id=cat.song_id JOIN SONG_VIDEO v ON v.song_id=s.song_id"
                                + " WHERE cat.is_active='Y' ORDER BY s.song_id",
                            r ->
                                    new Video(
                                            r.getLong(1),
                                            r.getString(2),
                                            r.getString(3),
                                            r.getString(4),
                                            r.getString(5),
                                            r.getString(6),
                                            r.getString(7),
                                            r.getString(8),
                                            r.getString(9),
                                            r.getString(10)));
        }
        long linked = rows.stream().filter(r -> r.videoId() != null).count(),
                played = rows.stream().filter(r -> r.status().equals("PLAYABLE")).count();
        var snapshot = new LinkedHashMap<String, Object>();
        snapshot.put(
                "checkedAt",
                rows.stream()
                        .map(Video::checkedAt)
                        .filter(Objects::nonNull)
                        .map(s -> s.substring(0, 10))
                        .max(String::compareTo)
                        .orElse("unverified"));
        snapshot.put("catalogCount", rows.size());
        snapshot.put("metadataVerified", linked);
        snapshot.put("playableVerified", played);
        snapshot.put(
                "note",
                "PLAYABLE is a dated in-app iframe or official watch-page playback check, not a"
                    + " guarantee of future regional availability. SEARCH and UNAVAILABLE are not"
                    + " claimed as playable.");
        snapshot.put("tracks", rows);
        Files.writeString(
                Path.of("docs/youtube-links.json"),
                new GsonBuilder()
                                .setPrettyPrinting()
                                .disableHtmlEscaping()
                                .create()
                                .toJson(snapshot)
                        + "\n");
        var sql =
                new StringBuilder(
                        "-- Public YouTube link metadata; no audio/video files or stream URLs.\n"
                                + "-- Run 13 first. Reimport preserves existing user-selected video"
                                + " links.\n"
                                + "SET DEFINE OFF;\n");
        for (var r : rows)
            if (r.videoId() != null)
                sql.append("UPDATE SONG_VIDEO SET youtube_video_id=")
                        .append(q(r.videoId()))
                        .append(",youtube_title=")
                        .append(q(r.title()))
                        .append(",channel_name=")
                        .append(q(r.channel()))
                        .append(",video_kind=")
                        .append(q(r.kind()))
                        .append(",match_status=")
                        .append(q(r.status()))
                        .append(",checked_at=TO_TIMESTAMP(")
                        .append(q(r.checkedAt()))
                        .append(",'YYYY-MM-DD HH24:MI:SS') WHERE song_id=")
                        .append(r.songId())
                        .append(" AND youtube_video_id IS NULL;\n");
        Files.writeString(Path.of("sql/14_youtube_links.sql"), sql.append("COMMIT;\n").toString());
        System.out.println(
                "Catalog="
                        + rows.size()
                        + "; linked="
                        + linked
                        + "; playback verified="
                        + played
                        + "; search="
                        + (rows.size() - linked)
                        + "; unavailable="
                        + (linked - played));
    }
}
