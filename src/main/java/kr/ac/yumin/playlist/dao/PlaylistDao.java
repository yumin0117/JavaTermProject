package kr.ac.yumin.playlist.dao;

import kr.ac.yumin.playlist.model.*;

import java.sql.*;
import java.util.*;

public final class PlaylistDao {
    private static final String SELECT =
            """
SELECT p.playlist_id,p.title,p.description,p.is_public,p.member_id,m.nickname,p.created_at,
  (SELECT COUNT(*) FROM PLAYLIST_LIKE l WHERE l.playlist_id=p.playlist_id) likes,
  (SELECT COUNT(*) FROM PLAYLIST_LIKE l WHERE l.playlist_id=p.playlist_id AND l.member_id=?) liked
FROM PLAYLIST p JOIN MEMBER m ON m.member_id=p.member_id
""";

    private Playlist map(ResultSet r) throws SQLException {
        return new Playlist(
                r.getLong(1),
                r.getString(2),
                r.getString(3),
                "Y".equals(r.getString(4)),
                r.getString(5),
                r.getString(6),
                r.getTimestamp(7).toLocalDateTime(),
                r.getInt(8),
                r.getInt(9) > 0);
    }

    public List<Playlist> mine(Connection c, String user) throws SQLException {
        return Jdbc.list(
                c,
                SELECT + " WHERE p.member_id=? ORDER BY p.created_at DESC,p.playlist_id DESC",
                this::map,
                user,
                user);
    }

    public List<Playlist> publicLists(Connection c, String user, String query) throws SQLException {
        return Jdbc.list(
                c,
                SELECT
                        + " WHERE p.is_public='Y' AND INSTR(LOWER(p.title),LOWER(?))>0 ORDER BY"
                        + " p.created_at DESC,p.playlist_id DESC",
                this::map,
                user,
                query);
    }

    public List<Playlist> allPublic(Connection c, String user) throws SQLException {
        return Jdbc.list(
                c,
                SELECT + " WHERE p.is_public='Y' ORDER BY p.created_at DESC,p.playlist_id DESC",
                this::map,
                user);
    }

    public Optional<Playlist> visible(Connection c, String user, long id) throws SQLException {
        return Jdbc.list(
                        c,
                        SELECT + " WHERE p.playlist_id=? AND (p.member_id=? OR p.is_public='Y')",
                        this::map,
                        user,
                        id,
                        user)
                .stream()
                .findFirst();
    }

    /** 잠금 후 소유자를 다시 검사한다. 모든 쓰기 작업이 같은 부모 행을 잠근다. */
    public boolean lockOwner(Connection c, String user, long id) throws SQLException {
        return !Jdbc.list(
                        c,
                        "SELECT playlist_id FROM PLAYLIST WHERE playlist_id=? AND member_id=? FOR"
                                + " UPDATE WAIT 5",
                        r -> r.getLong(1),
                        id,
                        user)
                .isEmpty();
    }

    public boolean lockPublic(Connection c, long id) throws SQLException {
        return !Jdbc.list(
                        c,
                        "SELECT playlist_id FROM PLAYLIST WHERE playlist_id=? AND is_public='Y' FOR"
                                + " UPDATE WAIT 5",
                        r -> r.getLong(1),
                        id)
                .isEmpty();
    }

    public long create(Connection c, String user, String title, String description, boolean pub)
            throws SQLException {
        long id = Jdbc.number(c, "SELECT SEQ_PLAYLIST.NEXTVAL FROM DUAL");
        Jdbc.update(
                c,
                "INSERT INTO PLAYLIST(playlist_id,title,description,is_public,member_id)"
                        + " VALUES(?,?,?,?,?)",
                id,
                title,
                description,
                pub ? "Y" : "N",
                user);
        return id;
    }

    public int edit(Connection c, String user, long id, String title, String desc, boolean pub)
            throws SQLException {
        return Jdbc.update(
                c,
                "UPDATE PLAYLIST SET title=?,description=?,is_public=? WHERE playlist_id=? AND"
                        + " member_id=?",
                title,
                desc,
                pub ? "Y" : "N",
                id,
                user);
    }

    public int delete(Connection c, String user, long id) throws SQLException {
        return Jdbc.update(c, "DELETE FROM PLAYLIST WHERE playlist_id=? AND member_id=?", id, user);
    }

    public void like(Connection c, String user, long id, boolean value) throws SQLException {
        if (value)
            Jdbc.update(
                    c,
                    """
INSERT INTO PLAYLIST_LIKE(member_id,playlist_id)
SELECT ?,playlist_id FROM PLAYLIST p WHERE p.playlist_id=? AND p.is_public='Y'
AND NOT EXISTS(SELECT 1 FROM PLAYLIST_LIKE l WHERE l.member_id=? AND l.playlist_id=p.playlist_id)
""",
                    user,
                    id,
                    user);
        else
            Jdbc.update(
                    c, "DELETE FROM PLAYLIST_LIKE WHERE member_id=? AND playlist_id=?", user, id);
    }
}
