package kr.ac.yumin.playlist.dao;

import kr.ac.yumin.playlist.model.*;

import java.sql.*;
import java.util.*;

public final class MediaDao {
    private static final String COLUMNS =
            """
SELECT s.song_id,s.title,
(SELECT LISTAGG(a.artist_name,', ') WITHIN GROUP(ORDER BY a.artist_id)
 FROM SONG_ARTIST sa JOIN ARTIST a ON a.artist_id=sa.artist_id WHERE sa.song_id=s.song_id),
s.album_title,s.genre,s.duration_seconds,m.resource_path,m.performer,m.source_url,m.license_name
""";

    public List<MediaTrack> catalog(Connection c) throws SQLException {
        return Jdbc.list(
                c,
                COLUMNS
                        + """
,NVL(e.chart_rank,0),cat.cover_url,TO_CHAR(cat.added_at,'YYYY.MM.DD HH24:MI')
FROM MUSIC_CATALOG cat JOIN SONG s ON s.song_id=cat.song_id
LEFT JOIN MELON_CHART_ENTRY e ON e.song_id=s.song_id AND e.chart_at=(SELECT MAX(chart_at) FROM MELON_CHART)
LEFT JOIN SONG_MEDIA m ON m.song_id=s.song_id
WHERE cat.is_active='Y'
ORDER BY NVL(e.chart_rank,1000000),cat.added_at,s.song_id
""",
                r ->
                        new MediaTrack(
                                song(r),
                                r.getString(7),
                                r.getString(8),
                                r.getString(9),
                                r.getString(10),
                                r.getInt(11),
                                r.getString(12),
                                r.getString(13)));
    }

    public MediaTrack find(Connection c, long id) throws SQLException {
        return Jdbc.list(
                        c,
                        COLUMNS
                                + ",0,cat.cover_url,NULL FROM SONG s LEFT JOIN SONG_MEDIA m ON"
                                + " m.song_id=s.song_id LEFT JOIN MUSIC_CATALOG cat ON"
                                + " cat.song_id=s.song_id WHERE s.song_id=?",
                        r ->
                                new MediaTrack(
                                        song(r),
                                        r.getString(7),
                                        r.getString(8),
                                        r.getString(9),
                                        r.getString(10),
                                        0,
                                        r.getString(12),
                                        null),
                        id)
                .stream()
                .findFirst()
                .orElse(null);
    }

    private Song song(ResultSet r) throws SQLException {
        return new Song(
                r.getLong(1),
                r.getString(2),
                r.getString(3),
                r.getString(4),
                r.getString(5),
                r.getInt(6),
                0);
    }
}
