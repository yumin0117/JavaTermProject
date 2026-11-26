package kr.ac.yumin.playlist.service;

import kr.ac.yumin.playlist.config.Database;
import kr.ac.yumin.playlist.dao.Jdbc;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;

import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;

/** Public chart metadata only. No streaming URLs or audio are collected. */
public final class MelonChartService {
    public static final String SOURCE = "https://www.melon.com/chart/index.htm";

    public record Artist(long id, String name) {}

    public record Entry(
            int rank,
            long melonId,
            String title,
            String album,
            String cover,
            List<Artist> artists) {
        public long songId() {
            return 1_000_000_000L + melonId;
        }
    }

    public record Snapshot(LocalDateTime chartAt, List<Entry> entries) {}

    public Snapshot fetch() throws Exception {
        return parse(
                Jsoup.connect(SOURCE)
                        .userAgent("Mozilla/5.0")
                        .referrer("https://www.melon.com/")
                        .timeout(30000)
                        .get());
    }

    public Snapshot read(Path path) throws Exception {
        return parse(Jsoup.parse(path.toFile(), "UTF-8", SOURCE));
    }

    public Snapshot parse(Document document) {
        try {
            LocalDateTime at =
                    LocalDateTime.parse(
                            document.selectFirst(".year").text()
                                    + " "
                                    + document.selectFirst(".hour").text(),
                            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"));
            List<Entry> entries = new ArrayList<>();
            Set<Long> songs = new HashSet<>();
            for (Element row : document.select("tr.lst50[data-song-no], tr.lst100[data-song-no]")) {
                int rank = Integer.parseInt(row.selectFirst(".rank").text());
                long id = Long.parseLong(row.attr("data-song-no"));
                String title = row.selectFirst(".rank01 a").text();
                String album = row.selectFirst(".rank03 a").text();
                Map<Long, Artist> artists = new LinkedHashMap<>();
                for (Element a : row.select(".rank02 a[href*=artistId]")) {
                    Matcher matcher =
                            Pattern.compile("artistId[=\\s]*([0-9]+)").matcher(a.attr("href"));
                    if (matcher.find()) {
                        long artistId = Long.parseLong(matcher.group(1));
                        artists.putIfAbsent(
                                artistId, new Artist(2_000_000_000L + artistId, a.text()));
                    }
                }
                if (rank != entries.size() + 1
                        || !songs.add(id)
                        || title.isBlank()
                        || album.isBlank()
                        || artists.isEmpty())
                    throw new IllegalArgumentException("차트 순위·중복·곡 정보 검증 실패");
                entries.add(
                        new Entry(
                                rank,
                                id,
                                title,
                                album,
                                row.selectFirst("img").absUrl("src"),
                                List.copyOf(artists.values())));
            }
            if (entries.size() != 100) throw new IllegalArgumentException("TOP100 전체 100곡이 필요합니다.");
            return new Snapshot(at, List.copyOf(entries));
        } catch (RuntimeException e) {
            throw new AppException("멜론 차트 형식이 바뀌었거나 100곡을 확인할 수 없습니다. 기존 DB 목록은 유지됩니다.");
        }
    }

    public void save(Snapshot snapshot) throws SQLException {
        if (snapshot.entries().size() != 100) throw new AppException("100곡을 확인한 차트만 저장할 수 있습니다.");
        try (Connection c = Database.open()) {
            c.setAutoCommit(false);
            try {
                Jdbc.update(c, "LOCK TABLE MELON_CHART IN EXCLUSIVE MODE WAIT 5");
                Timestamp at = Timestamp.valueOf(snapshot.chartAt());
                for (Entry e : snapshot.entries()) {
                    for (Artist a : e.artists()) {
                        Jdbc.update(
                                c,
                                "MERGE INTO ARTIST d USING(SELECT ? id FROM DUAL) x"
                                    + " ON(d.artist_id=x.id) WHEN MATCHED THEN UPDATE SET"
                                    + " artist_name=? WHEN NOT MATCHED THEN"
                                    + " INSERT(artist_id,artist_name,country) VALUES(x.id,?,'미확인')",
                                a.id(),
                                a.name(),
                                a.name());
                    }
                    Jdbc.update(
                            c,
                            "MERGE INTO SONG d USING(SELECT ? id FROM DUAL) x ON(d.song_id=x.id)"
                                    + " WHEN MATCHED THEN UPDATE SET title=?,album_title=? WHEN NOT"
                                    + " MATCHED THEN INSERT(song_id,title,album_title)"
                                    + " VALUES(x.id,?,?)",
                            e.songId(),
                            e.title(),
                            e.album(),
                            e.title(),
                            e.album());
                    for (Artist a : e.artists())
                        Jdbc.update(
                                c,
                                "MERGE INTO SONG_ARTIST d USING(SELECT ? s,? a FROM DUAL) x"
                                    + " ON(d.song_id=x.s AND d.artist_id=x.a) WHEN NOT MATCHED THEN"
                                    + " INSERT(song_id,artist_id) VALUES(x.s,x.a)",
                                e.songId(),
                                a.id());
                }
                Jdbc.update(
                        c,
                        "MERGE INTO MELON_CHART d USING(SELECT ? at FROM DUAL) x"
                                + " ON(d.chart_at=x.at) WHEN MATCHED THEN UPDATE SET"
                                + " fetched_at=SYSTIMESTAMP WHEN NOT MATCHED THEN"
                                + " INSERT(chart_at,source_url,row_count) VALUES(x.at,?,100)",
                        at,
                        SOURCE);
                for (Entry e : snapshot.entries())
                    Jdbc.update(
                            c,
                            "MERGE INTO MELON_CHART_ENTRY d USING(SELECT ? at,? rank FROM DUAL) x"
                                + " ON(d.chart_at=x.at AND d.chart_rank=x.rank) WHEN MATCHED THEN"
                                + " UPDATE SET song_id=?,melon_song_id=?,cover_url=? WHEN NOT"
                                + " MATCHED THEN"
                                + " INSERT(chart_at,chart_rank,song_id,melon_song_id,cover_url)"
                                + " VALUES(x.at,x.rank,?,?,?)",
                            at,
                            e.rank(),
                            e.songId(),
                            e.melonId(),
                            e.cover(),
                            e.songId(),
                            e.melonId(),
                            e.cover());
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        MelonChartService service = new MelonChartService();
        Snapshot snapshot = args.length == 0 ? service.fetch() : service.read(Path.of(args[0]));
        service.save(snapshot);
        System.out.println(
                "Oracle Melon TOP100: "
                        + snapshot.chartAt()
                        + " / "
                        + snapshot.entries().size()
                        + " songs");
    }
}
