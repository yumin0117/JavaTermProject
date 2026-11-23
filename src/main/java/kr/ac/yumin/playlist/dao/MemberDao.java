package kr.ac.yumin.playlist.dao;

import kr.ac.yumin.playlist.model.Member;

import java.sql.*;
import java.util.*;

public final class MemberDao {
    public record Credentials(Member member, String hash) {}

    public Optional<Credentials> find(Connection c, String id) throws SQLException {
        return Jdbc.list(
                        c,
                        "SELECT member_id,nickname,email,password_hash FROM MEMBER WHERE"
                            + " member_id=?",
                        r ->
                                new Credentials(
                                        new Member(r.getString(1), r.getString(2), r.getString(3)),
                                        r.getString(4)),
                        id)
                .stream()
                .findFirst();
    }

    public void insert(Connection c, Member m, String hash) throws SQLException {
        Jdbc.update(
                c,
                "INSERT INTO MEMBER(member_id,password_hash,nickname,email) VALUES(?,?,?,?)",
                m.id(),
                hash,
                m.nickname(),
                m.email());
    }
}
