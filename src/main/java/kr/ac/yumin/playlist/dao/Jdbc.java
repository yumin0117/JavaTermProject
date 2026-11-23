package kr.ac.yumin.playlist.dao;

import java.sql.*;
import java.util.*;

/** 한 연결에서 실행하며 모든 JDBC 자원을 try-with-resources로 닫는다. */
public final class Jdbc {
    private Jdbc() {}

    @FunctionalInterface
    public interface Mapper<T> {
        T read(ResultSet r) throws SQLException;
    }

    public static PreparedStatement prepare(Connection c, String sql, Object... args)
            throws SQLException {
        PreparedStatement p = c.prepareStatement(sql);
        try {
            p.setQueryTimeout(20);
            for (int i = 0; i < args.length; i++) p.setObject(i + 1, args[i]);
            return p;
        } catch (SQLException e) {
            p.close();
            throw e;
        }
    }

    public static int update(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement p = prepare(c, sql, args)) {
            return p.executeUpdate();
        }
    }

    public static <T> List<T> list(Connection c, String sql, Mapper<T> mapper, Object... args)
            throws SQLException {
        try (PreparedStatement p = prepare(c, sql, args);
                ResultSet r = p.executeQuery()) {
            List<T> out = new ArrayList<>();
            while (r.next()) out.add(mapper.read(r));
            return out;
        }
    }

    public static long number(Connection c, String sql, Object... args) throws SQLException {
        return list(c, sql, r -> r.getLong(1), args).get(0);
    }
}
