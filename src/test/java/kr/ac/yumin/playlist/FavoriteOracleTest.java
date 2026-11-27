package kr.ac.yumin.playlist;

import static org.junit.jupiter.api.Assertions.*;

import kr.ac.yumin.playlist.config.Database;
import kr.ac.yumin.playlist.dao.Jdbc;
import kr.ac.yumin.playlist.service.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;

@Tag("oracle")
class FavoriteOracleTest {
    @TempDir Path folder;

    @Test
    void detailUpdateRollsBackAllFactsWhenAnyCreditIsInvalid() throws Exception {
        AlbumService service = new AlbumService();
        var before = service.read(1603382333L);
        assertNotNull(before);
        var bad =
                new AlbumService.Facts(
                        before.track().song().id(),
                        before.albumId(),
                        before.title(),
                        before.artists(),
                        before.releaseDate(),
                        before.type(),
                        before.genre(),
                        before.distributor(),
                        "rollback probe",
                        before.coverUrl(),
                        java.util.List.of(
                                new kr.ac.yumin.playlist.model.AlbumDetail.Credit(
                                        "잘못", 1, "probe")));
        assertThrows(java.sql.SQLException.class, () -> service.save(bad));
        var after = service.read(1603382333L);
        assertEquals(before.agency(), after.agency());
        assertEquals(before.credits(), after.credits());
    }

    @Test
    void reimportingTheSameLatestSongsDoesNotDuplicateCatalog() throws Exception {
        String html;
        try (var in = getClass().getResourceAsStream("/melon-latest.html")) {
            html = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        var importer = new CatalogService();
        long before;
        long inactive;
        try (Connection c = Database.open()) {
            before = Jdbc.number(c, "SELECT COUNT(*) FROM MUSIC_CATALOG");
            inactive = Jdbc.number(c, "SELECT COUNT(*) FROM MUSIC_CATALOG WHERE is_active='N'");
        }
        assertEquals(
                0,
                importer.save(
                        importer.parseLatest(
                                org.jsoup.Jsoup.parse(html, "https://www.melon.com/"))));
        try (Connection c = Database.open()) {
            assertEquals(before, Jdbc.number(c, "SELECT COUNT(*) FROM MUSIC_CATALOG"));
            assertEquals(
                    inactive,
                    Jdbc.number(c, "SELECT COUNT(*) FROM MUSIC_CATALOG WHERE is_active='N'"));
        }
    }

    @Test
    void favoritesPersistAreIdempotentAndIsolatedBetweenMembers() throws Exception {
        var one = new LocalProfileService().resolve(folder.resolve("one.properties"));
        var two = new LocalProfileService().resolve(folder.resolve("two.properties"));
        long song = 1603382333L;
        try {
            FavoriteService first = new FavoriteService(one), second = new FavoriteService(two);
            first.set(song, true);
            first.set(song, true);
            assertEquals(1, new FavoriteService(one).ids().size());
            assertTrue(first.ids().contains(song));
            assertFalse(second.ids().contains(song));
            second.set(song, false);
            assertTrue(first.ids().contains(song));
            assertThrows(java.sql.SQLException.class, () -> first.set(9999999999L, true));
            assertEquals(1, first.ids().size());
            first.set(song, false);
            assertTrue(first.ids().isEmpty());
            var album = new AlbumService().read(song);
            assertNotNull(album);
            assertEquals("NEON", album.title());
            assertTrue(album.people("작곡").contains("Dem Jointz"));
        } finally {
            try (Connection c = Database.open()) {
                Jdbc.update(
                        c,
                        "DELETE FROM SONG_FAVORITE WHERE member_id IN (?,?)",
                        one.id(),
                        two.id());
                Jdbc.update(c, "DELETE FROM MEMBER WHERE member_id IN (?,?)", one.id(), two.id());
            }
        }
    }
}
