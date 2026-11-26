package kr.ac.yumin.playlist.service;

import kr.ac.yumin.playlist.config.Database;
import kr.ac.yumin.playlist.dao.*;
import kr.ac.yumin.playlist.model.*;

import java.sql.*;
import java.util.*;

/** 로그인 회원마다 생성한다. 모든 쓰기 작업은 부모 PLAYLIST 행 잠금과 같은 연결을 사용한다. */
public final class PlaylistService {
    private final Member member;
    private final PlaylistDao playlists = new PlaylistDao();
    private final SongDao songs = new SongDao();

    public PlaylistService(Member member) {
        this.member = Objects.requireNonNull(member);
    }

    public Member member() {
        return member;
    }

    @FunctionalInterface
    private interface Work<T> {
        T run(Connection c) throws SQLException;
    }

    private <T> T transaction(Work<T> action) throws SQLException {
        try (Connection c = Database.open()) {
            c.setAutoCommit(false);
            try {
                T value = action.run(c);
                c.commit();
                return value;
            } catch (SQLException | RuntimeException e) {
                try {
                    c.rollback();
                } catch (SQLException rollback) {
                    e.addSuppressed(rollback);
                }
                throw e;
            }
        }
    }

    private void owner(Connection c, long id) throws SQLException {
        if (!playlists.lockOwner(c, member.id(), id))
            throw new AppException("본인이 만든 플레이리스트만 변경할 수 있습니다.");
    }

    public List<Playlist> mine() throws SQLException {
        try (Connection c = Database.open()) {
            return playlists.mine(c, member.id());
        }
    }

    public List<Playlist> discover(String query) throws SQLException {
        String q = AuthService.text(query, "검색어", 150, false);
        try (Connection c = Database.open()) {
            return q.isEmpty()
                    ? playlists.allPublic(c, member.id())
                    : playlists.publicLists(c, member.id(), q);
        }
    }

    public List<Song> search(String query) throws SQLException {
        String q = AuthService.text(query, "검색어", 200, false);
        try (Connection c = Database.open()) {
            return songs.search(c, q);
        }
    }

    public List<Song> searchPlayable(String query) throws SQLException {
        String q = AuthService.text(query, "검색어", 200, false).toLowerCase(Locale.ROOT);
        try (Connection c = Database.open()) {
            return new MediaDao()
                    .catalog(c).stream()
                            .map(MediaTrack::song)
                            .filter(
                                    song ->
                                            (song.title() + " " + song.artists())
                                                    .toLowerCase(Locale.ROOT)
                                                    .contains(q))
                            .toList();
        }
    }

    public PlaylistDetail detail(long id) throws SQLException {
        try (Connection c = Database.open()) {
            // 두 SELECT가 동일 시점의 공개 여부와 수록곡을 보도록 읽기 전용 트랜잭션 사용.
            c.setAutoCommit(false);
            try {
                Jdbc.update(c, "SET TRANSACTION READ ONLY");
                Playlist p =
                        playlists
                                .visible(c, member.id(), id)
                                .orElseThrow(() -> new AppException("조회할 수 없는 플레이리스트입니다."));
                var list = songs.inPlaylist(c, member.id(), id);
                c.commit();
                return new PlaylistDetail(p, List.copyOf(list));
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public long create(String title, String description, boolean pub) throws SQLException {
        String t = AuthService.text(title, "제목", 150, true),
                d = AuthService.text(description, "설명", 1000, false);
        return transaction(c -> playlists.create(c, member.id(), t, d, pub));
    }

    public void edit(long id, String title, String description, boolean pub) throws SQLException {
        String t = AuthService.text(title, "제목", 150, true),
                d = AuthService.text(description, "설명", 1000, false);
        transaction(
                c -> {
                    owner(c, id);
                    playlists.edit(c, member.id(), id, t, d, pub);
                    return null;
                });
    }

    public void delete(long id) throws SQLException {
        transaction(
                c -> {
                    owner(c, id);
                    playlists.delete(c, member.id(), id);
                    return null;
                });
    }

    public void add(long id, long song) throws SQLException {
        transaction(
                c -> {
                    owner(c, id);
                    var list = songs.inPlaylist(c, member.id(), id);
                    if (list.stream().anyMatch(s -> s.id() == song))
                        throw new AppException("이미 추가한 곡입니다.");
                    int next = list.stream().mapToInt(Song::order).max().orElse(0) + 1;
                    if (next > 999999) throw new AppException("목록에 더 이상 곡을 추가할 수 없습니다.");
                    songs.add(c, member.id(), id, song, next);
                    return null;
                });
    }

    public void remove(long id, long song) throws SQLException {
        transaction(
                c -> {
                    owner(c, id);
                    if (songs.remove(c, member.id(), id, song) != 1)
                        throw new AppException("이미 제거된 곡입니다.");
                    var remaining = songs.inPlaylist(c, member.id(), id);
                    // 오름차순으로 빈 번호에 이동하므로 UNIQUE 위반 없이 1부터 정리된다.
                    for (int i = 0; i < remaining.size(); i++)
                        if (remaining.get(i).order() != i + 1)
                            songs.order(c, member.id(), id, remaining.get(i).id(), i + 1);
                    return null;
                });
    }

    public void move(long id, long song, int direction) throws SQLException {
        if (direction != 1 && direction != -1) throw new AppException("이동 방향이 올바르지 않습니다.");
        transaction(
                c -> {
                    owner(c, id);
                    var list = songs.inPlaylist(c, member.id(), id);
                    int index = -1;
                    for (int i = 0; i < list.size(); i++) if (list.get(i).id() == song) index = i;
                    if (index < 0) throw new AppException("목록에 없는 곡입니다.");
                    int other = index + direction;
                    if (other < 0 || other >= list.size()) return null;
                    Set<Integer> occupied = new HashSet<>();
                    for (Song s : list) occupied.add(s.order());
                    int temp = 1;
                    while (occupied.contains(temp) && temp <= 999999) temp++;
                    if (temp > 999999)
                        throw new AppException("순서를 바꿀 빈 번호가 없습니다. 곡 하나를 제거한 후 다시 시도해 주세요.");
                    Song a = list.get(index), b = list.get(other);
                    songs.order(c, member.id(), id, a.id(), temp);
                    songs.order(c, member.id(), id, b.id(), a.order());
                    songs.order(c, member.id(), id, a.id(), b.order());
                    return null;
                });
    }

    public void like(long id, boolean value) throws SQLException {
        transaction(
                c -> {
                    if (value && !playlists.lockPublic(c, id))
                        throw new AppException("공개 플레이리스트에만 좋아요를 누를 수 있습니다.");
                    playlists.like(c, member.id(), id, value);
                    return null;
                });
    }
}
