package kr.ac.yumin.playlist.dao;

import kr.ac.yumin.playlist.model.Song;

import java.sql.*;
import java.util.*;

public final class SongDao {
    // 검색 조건과 참여자 집계를 분리하여, 아티스트 한 명으로 검색해도 모든 참여자를 표시한다.
    private static final String COLUMNS =
            """
SELECT s.song_id,s.title,
  (SELECT LISTAGG(a.artist_name,', ' ON OVERFLOW TRUNCATE '...' WITHOUT COUNT)
   WITHIN GROUP(ORDER BY a.artist_id) FROM SONG_ARTIST sa JOIN ARTIST a ON a.artist_id=sa.artist_id
   WHERE sa.song_id=s.song_id) artists,s.album_title,s.genre,s.duration_seconds,
""";

    private Song map(ResultSet r) throws SQLException {
        return new Song(
                r.getLong(1),
                r.getString(2),
                r.getString(3),
                r.getString(4),
                r.getString(5),
                r.getInt(6),
                r.getInt(7));
    }

    public List<Song> search(Connection c, String query) throws SQLException {
        if (query.isEmpty())
            return Jdbc.list(c, COLUMNS + " 0 FROM SONG s ORDER BY s.title,s.song_id", this::map);
        return Jdbc.list(
                c,
                COLUMNS
                        + """
                          0 FROM SONG s WHERE INSTR(LOWER(s.title),LOWER(?))>0 OR EXISTS(
                            SELECT 1 FROM SONG_ARTIST sa JOIN ARTIST a ON a.artist_id=sa.artist_id
                            WHERE sa.song_id=s.song_id AND INSTR(LOWER(a.artist_name),LOWER(?))>0)
                          ORDER BY s.title,s.song_id
                          """,
                this::map,
                query,
                query);
    }

    public List<Song> inPlaylist(Connection c, String user, long id) throws SQLException {
        return Jdbc.list(
                c,
                COLUMNS
                        + """
ps.play_order FROM PLAYLIST_SONG ps JOIN SONG s ON s.song_id=ps.song_id
JOIN PLAYLIST p ON p.playlist_id=ps.playlist_id
WHERE ps.playlist_id=? AND (p.member_id=? OR p.is_public='Y') ORDER BY ps.play_order
""",
                this::map,
                id,
                user);
    }

    public void add(Connection c, String user, long id, long song, int order) throws SQLException {
        Jdbc.update(
                c,
                """
                INSERT INTO PLAYLIST_SONG(playlist_id,song_id,play_order)
                SELECT playlist_id,?,? FROM PLAYLIST WHERE playlist_id=? AND member_id=?
                """,
                song,
                order,
                id,
                user);
    }

    public int remove(Connection c, String user, long id, long song) throws SQLException {
        return Jdbc.update(
                c,
                """
DELETE FROM PLAYLIST_SONG ps WHERE playlist_id=? AND song_id=?
AND EXISTS(SELECT 1 FROM PLAYLIST p WHERE p.playlist_id=ps.playlist_id AND p.member_id=?)
""",
                id,
                song,
                user);
    }

    public void order(Connection c, String user, long id, long song, int order)
            throws SQLException {
        int n =
                Jdbc.update(
                        c,
                        """
UPDATE PLAYLIST_SONG ps SET play_order=? WHERE playlist_id=? AND song_id=?
AND EXISTS(SELECT 1 FROM PLAYLIST p WHERE p.playlist_id=ps.playlist_id AND p.member_id=?)
""",
                        order,
                        id,
                        song,
                        user);
        if (n != 1) throw new SQLException("대상 곡이 변경되었습니다. 새로고침 후 다시 시도해 주세요.");
    }
}
