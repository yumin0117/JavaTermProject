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
                          ,e.chart_rank,e.cover_url,TO_CHAR(e.chart_at,'YYYY.MM.DD HH24:MI')
                          FROM MELON_CHART_ENTRY e JOIN SONG s ON s.song_id=e.song_id
                          LEFT JOIN SONG_MEDIA m ON m.song_id=s.song_id
                          WHERE e.chart_at=(SELECT MAX(chart_at) FROM MELON_CHART)
                          ORDER BY e.chart_rank
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
                                + " FROM SONG s LEFT JOIN SONG_MEDIA m ON m.song_id=s.song_id WHERE"
                                + " s.song_id=?",
                        r ->
                                new MediaTrack(
                                        song(r),
                                        r.getString(7),
                                        r.getString(8),
                                        r.getString(9),
                                        r.getString(10)),
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
