package kr.ac.yumin.playlist.service;

import kr.ac.yumin.playlist.config.Database;
import kr.ac.yumin.playlist.dao.*;
import kr.ac.yumin.playlist.model.*;
import kr.ac.yumin.playlist.model.AlbumDetail.Credit;

import org.jsoup.nodes.*;

import java.sql.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

/** Stores album facts and credits only; lyrics and audio are never collected. */
public final class AlbumService {
    public record Facts(
            long songId,
            long albumId,
            String title,
            String artists,
            LocalDate releaseDate,
            String type,
            String genre,
            String distributor,
            String agency,
            String coverUrl,
            List<Credit> credits) {}

    static String meta(Document d, String label) {
        for (Element dt : d.select(".section_info dt")) {
            Element next = dt.nextElementSibling();
            if (dt.text().equals(label) && next != null && next.tagName().equals("dd"))
                return next.text();
        }
        return "";
    }

    private long albumId(Document song) {
        for (Element a : song.select(".section_info .meta a[href]")) {
            if (a.attr("href").contains("goAlbumDetail"))
                return CatalogService.id(a.attr("href"), "goAlbumDetail\\(['\"]?([0-9]+)");
        }
        throw new AppException("앨범 번호를 확인할 수 없습니다.");
    }

    public Facts parse(long songId, Document song, Document album) {
        long albumId = albumId(song);
        if (song.selectFirst(".song_name") == null) throw new AppException("곡 상세 정보를 확인할 수 없습니다.");
        List<Credit> credits = new ArrayList<>();
        Map<String, Integer> orders = new HashMap<>();
        for (Element li : song.select(".list_person li")) {
            Element role = li.selectFirst(".type"), name = li.selectFirst(".artist_name");
            if (role == null || name == null || name.text().isBlank()) continue;
            String r = role.text();
            if (!Set.of("작사", "작곡", "편곡").contains(r)) continue;
            int order = orders.merge(r, 1, Integer::sum);
            credits.add(new Credit(r, order, name.text()));
        }
        String title = meta(song, "앨범"), date = meta(album, "발매일");
        if (title.isBlank()) throw new AppException("앨범 제목을 확인할 수 없습니다.");
        LocalDate release = null;
        if (!date.isBlank()) {
            try {
                release = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy.MM.dd"));
            } catch (DateTimeParseException e) {
                throw new AppException("발매일 형식을 확인할 수 없습니다.");
            }
        }
        String artists =
                album.select(".section_info .artist .artist_name").eachText().stream()
                        .distinct()
                        .collect(java.util.stream.Collectors.joining(", "));
        Element type = album.selectFirst(".section_info .gubun"),
                cover = album.selectFirst(".section_info .image_typeAll img");
        return new Facts(
                songId,
                albumId,
                title,
                artists,
                release,
                type == null ? null : type.text(),
                meta(album, "장르"),
                meta(album, "발매사"),
                meta(album, "기획사"),
                cover == null ? null : cover.absUrl("src"),
                List.copyOf(credits));
    }

    public AlbumDetail load(long songId, boolean refresh) throws Exception {
        AlbumDetail cached = read(songId);
        if (cached != null && !refresh) return cached;
        try (Connection c = Database.open()) {
            if (Jdbc.number(c, "SELECT COUNT(*) FROM MUSIC_CATALOG WHERE song_id=?", songId) == 0)
                throw new AppException("음악 보관함에 등록된 곡만 상세 정보를 불러올 수 있습니다.");
        }
        Document song =
                CatalogService.page(
                        "https://www.melon.com/song/detail.htm?songId="
                                + (songId - 1_000_000_000L));
        long albumId = albumId(song);
        Document album =
                CatalogService.page("https://www.melon.com/album/detail.htm?albumId=" + albumId);
        save(parse(songId, song, album));
        return read(songId);
    }

    public void save(Facts f) throws SQLException {
        try (Connection c = Database.open()) {
            c.setAutoCommit(false);
            try {
                // All detail writers take the same lock so shared albums cannot race on first
                // insert.
                Jdbc.update(c, "LOCK TABLE ALBUM_INFO IN EXCLUSIVE MODE WAIT 5");
                java.sql.Date date =
                        f.releaseDate() == null ? null : java.sql.Date.valueOf(f.releaseDate());
                String source = "https://www.melon.com/album/detail.htm?albumId=" + f.albumId();
                Jdbc.update(
                        c,
                        "MERGE INTO ALBUM_INFO d USING(SELECT ? id FROM DUAL) x ON(d.album_id=x.id)"
                            + " WHEN MATCHED THEN UPDATE SET"
                            + " album_title=?,album_artists=?,release_date=?,album_type=?,genre=?,distributor=?,agency=?,cover_url=?,fetched_at=SYSTIMESTAMP"
                            + " WHEN NOT MATCHED THEN"
                            + " INSERT(album_id,album_title,album_artists,release_date,album_type,genre,distributor,agency,cover_url,source_url)"
                            + " VALUES(x.id,?,?,?,?,?,?,?,?,?)",
                        f.albumId(),
                        f.title(),
                        f.artists(),
                        date,
                        f.type(),
                        f.genre(),
                        f.distributor(),
                        f.agency(),
                        f.coverUrl(),
                        f.title(),
                        f.artists(),
                        date,
                        f.type(),
                        f.genre(),
                        f.distributor(),
                        f.agency(),
                        f.coverUrl(),
                        source);
                Jdbc.update(
                        c,
                        "MERGE INTO SONG_DETAIL d USING(SELECT ? id FROM DUAL) x ON(d.song_id=x.id)"
                                + " WHEN MATCHED THEN UPDATE SET album_id=?,fetched_at=SYSTIMESTAMP"
                                + " WHEN NOT MATCHED THEN INSERT(song_id,album_id,source_url)"
                                + " VALUES(x.id,?,?)",
                        f.songId(),
                        f.albumId(),
                        f.albumId(),
                        "https://www.melon.com/song/detail.htm?songId="
                                + (f.songId() - 1_000_000_000L));
                Jdbc.update(c, "DELETE FROM SONG_CREDIT WHERE song_id=?", f.songId());
                for (Credit credit : f.credits())
                    Jdbc.update(
                            c,
                            "INSERT INTO SONG_CREDIT(song_id,credit_role,credit_order,person_name)"
                                    + " VALUES(?,?,?,?)",
                            f.songId(),
                            credit.role(),
                            credit.order(),
                            credit.name());
                Jdbc.update(
                        c,
                        "UPDATE SONG SET release_date=?,genre=? WHERE song_id=?",
                        date,
                        f.genre(),
                        f.songId());
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public AlbumDetail read(long songId) throws SQLException {
        try (Connection c = Database.open()) {
            c.setAutoCommit(false);
            try {
                Jdbc.update(c, "SET TRANSACTION READ ONLY");
                MediaTrack track = new MediaDao().find(c, songId);
                List<Credit> credits =
                        Jdbc.list(
                                c,
                                "SELECT credit_role,credit_order,person_name FROM SONG_CREDIT WHERE"
                                        + " song_id=? ORDER BY credit_role,credit_order",
                                r -> new Credit(r.getString(1), r.getInt(2), r.getString(3)),
                                songId);
                List<Song> songs =
                        Jdbc.list(
                                c,
                                "SELECT s.song_id,s.title,s.album_title,s.genre,s.duration_seconds"
                                    + " FROM SONG s JOIN SONG_DETAIL d ON d.song_id=s.song_id WHERE"
                                    + " d.album_id=(SELECT album_id FROM SONG_DETAIL WHERE"
                                    + " song_id=?) ORDER BY s.song_id",
                                r ->
                                        new Song(
                                                r.getLong(1),
                                                r.getString(2),
                                                "",
                                                r.getString(3),
                                                r.getString(4),
                                                r.getInt(5),
                                                0),
                                songId);
                var result =
                        Jdbc.list(
                                        c,
                                        "SELECT"
                                            + " a.album_id,a.album_title,a.album_artists,a.release_date,a.album_type,a.genre,a.distributor,a.agency,a.cover_url,a.source_url,d.fetched_at"
                                            + " FROM SONG_DETAIL d JOIN ALBUM_INFO a ON"
                                            + " a.album_id=d.album_id WHERE d.song_id=?",
                                        r ->
                                                new AlbumDetail(
                                                        track,
                                                        r.getLong(1),
                                                        r.getString(2),
                                                        r.getString(3),
                                                        r.getDate(4) == null
                                                                ? null
                                                                : r.getDate(4).toLocalDate(),
                                                        r.getString(5),
                                                        r.getString(6),
                                                        r.getString(7),
                                                        r.getString(8),
                                                        r.getString(9),
                                                        r.getString(10),
                                                        r.getTimestamp(11).toLocalDateTime(),
                                                        credits,
                                                        songs),
                                        songId)
                                .stream()
                                .findFirst()
                                .orElse(null);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        AlbumService service = new AlbumService();
        List<Long> ids;
        try (Connection c = Database.open()) {
            ids =
                    args.length == 0
                            ? Jdbc.list(
                                    c,
                                    "SELECT song_id FROM MUSIC_CATALOG WHERE song_id NOT IN(SELECT"
                                            + " song_id FROM SONG_DETAIL) ORDER BY song_id",
                                    r -> r.getLong(1))
                            : List.of(Long.parseLong(args[0]));
        }
        int success = 0, failed = 0;
        for (long id : ids) {
            try {
                var detail = service.load(id, false);
                System.out.println("Saved album facts: " + id + " / " + detail.releaseDate());
                success++;
            } catch (Exception e) {
                System.out.println(
                        "Metadata unavailable: " + id + " / " + e.getClass().getSimpleName());
                failed++;
            }
            Thread.sleep(350);
        }
        System.out.println("Album facts saved=" + success + " unavailable=" + failed);
    }
}
