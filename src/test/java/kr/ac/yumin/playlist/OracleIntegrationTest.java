package kr.ac.yumin.playlist;

import static org.junit.jupiter.api.Assertions.*;

import kr.ac.yumin.playlist.config.Database;
import kr.ac.yumin.playlist.dao.Jdbc;
import kr.ac.yumin.playlist.model.*;
import kr.ac.yumin.playlist.service.*;

import org.junit.jupiter.api.*;

import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

/** mvn -Poracle-test test : 실제 Oracle, 기존 행은 변경하지 않고 신규 테스트 행만 정리한다. */
@Tag("oracle")
class OracleIntegrationTest {
    @Test
    void jdbcWorkflowPermissionsRollbackConcurrencyAndPreservation() throws Exception {
        String suffix = Long.toUnsignedString(System.nanoTime(), 36),
                a = "qa_a_" + suffix,
                b = "qa_b_" + suffix;
        long originalSongs, originalArtists;
        List<String> before;
        try (Connection c = Database.open()) {
            originalSongs = Jdbc.number(c, "SELECT COUNT(*) FROM SONG");
            originalArtists = Jdbc.number(c, "SELECT COUNT(*) FROM ARTIST");
            before = baseline(c);
        }
        AuthService auth = new AuthService();
        List<Long> created = new ArrayList<>();
        try {
            auth.register(
                    a,
                    "qa-password".toCharArray(),
                    "qa-password".toCharArray(),
                    "검증 회원 A",
                    "qa@example.com");
            auth.register(
                    b,
                    "qa-password".toCharArray(),
                    "qa-password".toCharArray(),
                    "검증 회원 B",
                    "qa@example.com");
            assertFalse(auth.available(a));
            assertThrows(
                    AppException.class,
                    () ->
                            auth.register(
                                    a,
                                    "a".toCharArray(),
                                    "a".toCharArray(),
                                    "중복",
                                    "qa@example.com"));
            assertThrows(AppException.class, () -> auth.login(a, "wrong".toCharArray()));
            assertThrows(
                    AppException.class,
                    () -> auth.login("user01", "DEMO_HASH_NOT_FOR_AUTH_01".toCharArray()));
            PlaylistService owner = new PlaylistService(auth.login(a, "qa-password".toCharArray()));
            PlaylistService other = new PlaylistService(auth.login(b, "qa-password".toCharArray()));
            long id = owner.create("JDBC 검증", "기존 데이터 보존", false);
            created.add(id);
            assertFalse(owner.detail(id).playlist().isPublic());
            assertTrue(other.mine().isEmpty());
            assertThrows(AppException.class, () -> other.detail(id));
            assertThrows(AppException.class, () -> other.edit(id, "침입", "", true));
            assertThrows(AppException.class, () -> other.add(id, 101));
            assertThrows(AppException.class, () -> other.remove(id, 101));
            assertThrows(AppException.class, () -> other.move(id, 101, 1));
            assertThrows(AppException.class, () -> other.delete(id));
            assertThrows(AppException.class, () -> other.like(id, true));
            owner.add(id, 101);
            owner.add(id, 102);
            owner.add(id, 103);
            assertThrows(AppException.class, () -> owner.add(id, 101));
            assertEquals(List.of(101L, 102L, 103L), ids(owner.detail(id)));
            owner.move(id, 103, -1);
            assertEquals(List.of(101L, 103L, 102L), ids(owner.detail(id)));
            owner.move(id, 101, 1);
            assertEquals(List.of(103L, 101L, 102L), ids(owner.detail(id)));
            // 중간 순서 UPDATE 오류가 나도 전체 트랜잭션을 롤백하는지 검증.
            try (Connection c = Database.open()) {
                c.setAutoCommit(false);
                Jdbc.list(
                        c,
                        "SELECT playlist_id FROM PLAYLIST WHERE playlist_id=? FOR UPDATE",
                        r -> r.getLong(1),
                        id);
                Jdbc.update(
                        c,
                        "UPDATE PLAYLIST_SONG SET play_order=999999 WHERE playlist_id=? AND"
                            + " song_id=103",
                        id);
                assertThrows(
                        SQLException.class,
                        () ->
                                Jdbc.update(
                                        c,
                                        "UPDATE PLAYLIST_SONG SET play_order=999999 WHERE"
                                            + " playlist_id=? AND song_id=101",
                                        id));
                c.rollback();
            }
            assertEquals(List.of(103L, 101L, 102L), ids(owner.detail(id)));
            owner.remove(id, 101);
            assertEquals(
                    List.of(1, 2), owner.detail(id).songs().stream().map(Song::order).toList());
            assertTrue(
                    owner.search("네온").stream()
                            .filter(s -> s.id() == 101)
                            .findFirst()
                            .orElseThrow()
                            .artists()
                            .contains("루나"));
            owner.edit(id, "공개 JDBC 검증", "공개 후 좋아요", true);
            assertEquals(2, other.detail(id).songs().size());
            owner.like(id, true);
            other.like(id, true);
            other.like(id, true);
            assertEquals(2, owner.detail(id).playlist().likes());
            assertTrue(other.detail(id).playlist().liked());
            other.like(id, false);
            assertEquals(1, owner.detail(id).playlist().likes());
            // 같은 목록에 대한 동시 추가: 한 연결만 성공하고 나머지는 중복 안내.
            ExecutorService pool = Executors.newFixedThreadPool(2);
            try {
                Callable<Boolean> add =
                        () -> {
                            try {
                                owner.add(id, 104);
                                return true;
                            } catch (AppException e) {
                                return false;
                            }
                        };
                var results = pool.invokeAll(List.of(add, add));
                assertNotEquals(results.get(0).get(), results.get(1).get());
                Callable<Boolean> move =
                        () -> {
                            owner.move(id, 104, -1);
                            return true;
                        };
                for (var result : pool.invokeAll(List.of(move, move))) assertTrue(result.get());
            } finally {
                pool.shutdownNow();
            }
            assertEquals(
                    List.of(1, 2, 3), owner.detail(id).songs().stream().map(Song::order).toList());
            owner.edit(id, "다시 비공개", "", false);
            assertThrows(AppException.class, () -> other.detail(id));
            assertThrows(AppException.class, () -> other.like(id, true));
            owner.like(id, false);
            owner.delete(id);
            try (Connection c = Database.open()) {
                assertEquals(
                        0,
                        Jdbc.number(
                                c, "SELECT COUNT(*) FROM PLAYLIST_SONG WHERE playlist_id=?", id));
                assertEquals(
                        0,
                        Jdbc.number(
                                c, "SELECT COUNT(*) FROM PLAYLIST_LIKE WHERE playlist_id=?", id));
                assertEquals(originalSongs, Jdbc.number(c, "SELECT COUNT(*) FROM SONG"));
                assertEquals(originalArtists, Jdbc.number(c, "SELECT COUNT(*) FROM ARTIST"));
            }
        } finally {
            try (Connection c = Database.open()) {
                for (long id : created)
                    Jdbc.update(
                            c, "DELETE FROM PLAYLIST WHERE playlist_id=? AND member_id=?", id, a);
                Jdbc.update(c, "DELETE FROM MEMBER WHERE member_id IN (?,?)", a, b);
                assertEquals(before, baseline(c), "기존 예시 데이터가 보존되어야 합니다.");
            }
        }
    }

    private static List<Long> ids(PlaylistDetail d) {
        return d.songs().stream().map(Song::id).toList();
    }

    private static List<String> baseline(Connection c) throws SQLException {
        List<String> rows = new ArrayList<>();
        for (String table :
                List.of(
                        "MEMBER",
                        "ARTIST",
                        "SONG",
                        "PLAYLIST",
                        "SONG_ARTIST",
                        "PLAYLIST_SONG",
                        "PLAYLIST_LIKE"))
            rows.addAll(
                    Jdbc.list(
                            c,
                            "SELECT * FROM " + table,
                            r -> {
                                StringBuilder s = new StringBuilder(table);
                                for (int i = 1; i <= r.getMetaData().getColumnCount(); i++)
                                    s.append('|').append(r.getString(i));
                                return s.toString();
                            }));
        Collections.sort(rows);
        return rows;
    }
}
