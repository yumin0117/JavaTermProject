package kr.ac.yumin.playlist.service;

import kr.ac.yumin.playlist.config.Database;
import kr.ac.yumin.playlist.dao.Jdbc;
import kr.ac.yumin.playlist.model.Member;

import java.sql.*;
import java.util.*;

public final class FavoriteService {
    private final Member member;

    public FavoriteService(Member member) {
        this.member = Objects.requireNonNull(member);
    }

    public Set<Long> ids() throws SQLException {
        try (Connection c = Database.open()) {
            return Set.copyOf(
                    Jdbc.list(
                            c,
                            "SELECT song_id FROM SONG_FAVORITE WHERE member_id=?",
                            r -> r.getLong(1),
                            member.id()));
        }
    }

    public void set(long songId, boolean favorite) throws SQLException {
        try (Connection c = Database.open()) {
            c.setAutoCommit(false);
            try {
                if (Jdbc.list(
                                c,
                                "SELECT member_id FROM MEMBER WHERE member_id=? FOR UPDATE WAIT 5",
                                r -> r.getString(1),
                                member.id())
                        .isEmpty()) throw new AppException("개인 보관함을 다시 연결해 주세요.");
                if (favorite)
                    Jdbc.update(
                            c,
                            "MERGE INTO SONG_FAVORITE d USING(SELECT ? m,? s FROM DUAL) x"
                                    + " ON(d.member_id=x.m AND d.song_id=x.s) WHEN NOT MATCHED THEN"
                                    + " INSERT(member_id,song_id) VALUES(x.m,x.s)",
                            member.id(),
                            songId);
                else
                    Jdbc.update(
                            c,
                            "DELETE FROM SONG_FAVORITE WHERE member_id=? AND song_id=?",
                            member.id(),
                            songId);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }
}
