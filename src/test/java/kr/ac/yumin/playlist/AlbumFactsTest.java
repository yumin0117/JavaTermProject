package kr.ac.yumin.playlist;

import static org.junit.jupiter.api.Assertions.*;

import kr.ac.yumin.playlist.service.*;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

class AlbumFactsTest {
    private org.jsoup.nodes.Document fixture(String file) throws Exception {
        try (var in = getClass().getResourceAsStream("/" + file)) {
            return Jsoup.parse(
                    new String(in.readAllBytes(), StandardCharsets.UTF_8),
                    "https://www.melon.com/");
        }
    }

    @Test
    void parsesRealReleaseAndSeparateWriterComposerCredits() throws Exception {
        var facts =
                new AlbumService()
                        .parse(
                                1603382333L,
                                fixture("song-neon-facts.html"),
                                fixture("album-neon-facts.html"));
        assertEquals("NEON", facts.title());
        assertEquals(14644795, facts.albumId());
        assertEquals(LocalDate.of(2026, 10, 13), facts.releaseDate());
        assertEquals("A2O, A2O LITE", facts.artists());
        assertTrue(
                facts.credits().stream()
                        .anyMatch(c -> c.role().equals("작사") && c.name().equals("유영진")));
        assertTrue(
                facts.credits().stream()
                        .anyMatch(c -> c.role().equals("작곡") && c.name().equals("Dem Jointz")));
    }

    @Test
    void rejectsMissingAlbumLinkAndParsesLatestWithoutDuplicateArtists() throws Exception {
        var invalid = fixture("song-neon-facts.html");
        invalid.select(".meta a").remove();
        assertThrows(
                AppException.class,
                () ->
                        new AlbumService()
                                .parse(1603382333L, invalid, fixture("album-neon-facts.html")));
        var rows = new CatalogService().parseLatest(fixture("melon-latest.html"));
        assertEquals(50, rows.size());
        assertEquals(rows.size(), rows.stream().map(e -> e.songId()).distinct().count());
        assertEquals(1, rows.get(0).artists().size());
    }
}
