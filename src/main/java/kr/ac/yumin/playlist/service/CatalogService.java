package kr.ac.yumin.playlist.service;

import kr.ac.yumin.playlist.config.Database;
import kr.ac.yumin.playlist.dao.Jdbc;
import kr.ac.yumin.playlist.service.MelonChartService.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;

import java.sql.*;
import java.util.*;
import java.util.regex.*;

public final class CatalogService {
    public static final String LATEST = "https://www.melon.com/new/index.htm";

    static Document page(String url) throws Exception {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .referrer("https://www.melon.com/")
                .timeout(15000)
                .maxBodySize(3_000_000)
                .get();
    }

    static long id(String text, String expression) {
        Matcher m = Pattern.compile(expression).matcher(text);
        if (!m.find()) throw new AppException("원본 곡 정보를 확인할 수 없습니다.");
        return Long.parseLong(m.group(1));
    }

    public List<Entry> parseLatest(Document document) {
        List<Entry> songs = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (Element row : document.select("tr")) {
            Element title = row.selectFirst(".rank01 a");
            if (title == null) continue;
            long song = id(title.attr("href"), "playSong\\([^,]+,\\s*([0-9]+)");
            Element album = row.selectFirst(".rank03 a");
            Map<Long, Artist> artists = new LinkedHashMap<>();
            for (Element a : row.select(".rank02 a")) {
                long aid = id(a.attr("href"), "goArtistDetail\\(['\"]?([0-9]+)");
                artists.putIfAbsent(aid, new Artist(2_000_000_000L + aid, a.text()));
            }
            if (album == null || title.text().isBlank() || artists.isEmpty() || !seen.add(song))
                throw new AppException("최신곡 필수 정보 또는 중복 확인 실패");
            songs.add(
                    new Entry(
                            songs.size() + 1,
                            song,
                            title.text(),
                            album.text(),
                            row.selectFirst("img").absUrl("src"),
                            List.copyOf(artists.values())));
        }
        if (songs.size() != 50) throw new AppException("최신곡 50곡을 확인할 수 없습니다. 기존 보관함은 유지됩니다.");
        return List.copyOf(songs);
    }

    public int importLatest() throws Exception {
        return save(parseLatest(page(LATEST)));
    }

    public int importSong(String value) throws Exception {
        String text = value.strip();
        if (!text.matches(
                "[0-9]{1,10}|https://www[.]melon[.]com/song/detail[.]htm[?]songId=[0-9]{1,10}"))
            throw new AppException("멜론 곡 상세 URL 또는 숫자 곡 번호를 입력해 주세요.");
        long id =
                Long.parseLong(
                        text.matches("[0-9]+")
                                ? text
                                : text.substring(text.indexOf("songId=") + 7));
        Document d = page("https://www.melon.com/song/detail.htm?songId=" + id);
        Element name = d.selectFirst(".song_name");
        if (name == null) throw new AppException("해당 곡 정보를 찾을 수 없습니다.");
        Element clean = name.clone();
        clean.select(".none").remove();
        Map<Long, Artist> artists = new LinkedHashMap<>();
        for (Element a : d.select(".section_info .artist a.artist_name")) {
            long aid = id(a.attr("href"), "goArtistDetail\\(['\"]?([0-9]+)");
            artists.putIfAbsent(
                    aid, new Artist(2_000_000_000L + aid, a.selectFirst("span").text()));
        }
        String album = AlbumService.meta(d, "앨범");
        Element img = d.selectFirst(".section_info .image_typeAll img");
        if (clean.text().isBlank() || album.isBlank() || artists.isEmpty())
            throw new AppException("곡 필수 정보를 확인할 수 없습니다.");
        int added =
                save(
                        List.of(
                                new Entry(
                                        0,
                                        id,
                                        clean.text(),
                                        album,
                                        img == null ? null : img.absUrl("src"),
                                        List.copyOf(artists.values()))));
        return added;
    }

    public int save(List<Entry> entries) throws SQLException {
        try (Connection c = Database.open()) {
            c.setAutoCommit(false);
            try {
                Jdbc.update(c, "LOCK TABLE MUSIC_CATALOG IN EXCLUSIVE MODE WAIT 5");
                int added = 0;
                for (Entry e : entries) {
                    boolean exists =
                            Jdbc.number(
                                            c,
                                            "SELECT COUNT(*) FROM MUSIC_CATALOG WHERE song_id=?",
                                            e.songId())
                                    > 0;
                    upsert(c, e);
                    if (!exists) added++;
                }
                c.commit();
                return added;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public static void upsert(Connection c, Entry e) throws SQLException {
        for (Artist a : e.artists())
            Jdbc.update(
                    c,
                    "MERGE INTO ARTIST d USING(SELECT ? id FROM DUAL) x ON(d.artist_id=x.id) WHEN"
                            + " MATCHED THEN UPDATE SET artist_name=? WHEN NOT MATCHED THEN"
                            + " INSERT(artist_id,artist_name,country) VALUES(x.id,?,'미확인')",
                    a.id(),
                    a.name(),
                    a.name());
        Jdbc.update(
                c,
                "MERGE INTO SONG d USING(SELECT ? id FROM DUAL) x ON(d.song_id=x.id) WHEN MATCHED"
                        + " THEN UPDATE SET title=?,album_title=? WHEN NOT MATCHED THEN"
                        + " INSERT(song_id,title,album_title) VALUES(x.id,?,?)",
                e.songId(),
                e.title(),
                e.album(),
                e.title(),
                e.album());
        for (Artist a : e.artists())
            Jdbc.update(
                    c,
                    "MERGE INTO SONG_ARTIST d USING(SELECT ? s,? a FROM DUAL) x ON(d.song_id=x.s"
                        + " AND d.artist_id=x.a) WHEN NOT MATCHED THEN INSERT(song_id,artist_id)"
                        + " VALUES(x.s,x.a)",
                    e.songId(),
                    a.id());
        Jdbc.update(
                c,
                "MERGE INTO MUSIC_CATALOG d USING(SELECT ? id FROM DUAL) x ON(d.song_id=x.id) WHEN"
                        + " MATCHED THEN UPDATE SET cover_url=? WHEN NOT MATCHED THEN"
                        + " INSERT(song_id,melon_song_id,cover_url) VALUES(x.id,?,?)",
                e.songId(),
                e.cover(),
                e.melonId(),
                e.cover());
    }

    public static void main(String[] args) throws Exception {
        CatalogService s = new CatalogService();
        int n = args.length == 0 ? s.importLatest() : s.importSong(args[0]);
        System.out.println("New catalog songs: " + n);
    }
}
