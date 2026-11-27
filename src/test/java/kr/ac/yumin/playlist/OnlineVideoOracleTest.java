package kr.ac.yumin.playlist;

import static org.junit.jupiter.api.Assertions.*;

import kr.ac.yumin.playlist.config.Database;
import kr.ac.yumin.playlist.dao.Jdbc;
import kr.ac.yumin.playlist.service.OnlineVideoService;

import org.junit.jupiter.api.*;

@Tag("oracle")
class OnlineVideoOracleTest {
    @Test
    void everyCatalogSongHasPersistentVideoMetadataAndFkProtectsSongIdentity() throws Exception {
        try (var c = Database.open()) {
            assertEquals(
                    Jdbc.number(c, "SELECT COUNT(*) FROM MUSIC_CATALOG"),
                    Jdbc.number(
                            c,
                            "SELECT COUNT(*) FROM MUSIC_CATALOG cat JOIN SONG_VIDEO v ON"
                                    + " v.song_id=cat.song_id"));
            c.setAutoCommit(false);
            try {
                assertThrows(
                        java.sql.SQLException.class,
                        () ->
                                Jdbc.update(
                                        c,
                                        "INSERT INTO SONG_VIDEO(song_id,search_query)"
                                                + " VALUES(-999,'missing song')"));
            } finally {
                c.rollback();
            }
        }
        var link = new OnlineVideoService().resolve(1603382333L);
        assertEquals("D968qfUZMUE", link.videoId());
        assertEquals("MV", link.kind());
    }
}
