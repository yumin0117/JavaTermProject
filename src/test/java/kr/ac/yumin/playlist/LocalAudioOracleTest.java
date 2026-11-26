package kr.ac.yumin.playlist;

import static org.junit.jupiter.api.Assertions.*;

import kr.ac.yumin.playlist.config.Database;
import kr.ac.yumin.playlist.dao.Jdbc;
import kr.ac.yumin.playlist.service.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.sql.*;

@Tag("oracle")
class LocalAudioOracleTest {
    @TempDir Path directory;

    @Test
    void connectsOwnedMp3AndKeepsChartMetadataSeparate() throws Exception {
        long id = 987654321;
        String resource = null;
        var profile = new LocalProfileService().resolve(directory.resolve("profile.properties"));
        try {
            try (Connection c = Database.open()) {
                Jdbc.update(
                        c,
                        "INSERT INTO SONG(song_id,title,album_title) VALUES(?,'강아지 왈츠 음원 연결"
                                + " 검증','CC0 검증')",
                        id);
            }
            Path source = directory.resolve("waltz.mp3");
            try (var in = getClass().getResourceAsStream("/audio/minute-waltz.mp3")) {
                Files.copy(in, source);
            }
            new LocalAudioService().attach(id, source);
            var authorized = new MusicLibraryService(profile).authorize(id);
            resource = authorized.track().resource();
            assertTrue(resource.matches("local-audio/[a-f0-9-]+[.]mp3"));
            assertEquals(108, authorized.track().song().seconds());
            assertEquals(60, authorized.pass().limitSeconds(108));
            assertTrue(Files.exists(Path.of("config/audio", resource.substring(12))));
            assertThrows(
                    AppException.class,
                    () -> new MusicLibraryService(profile).authorize(1_602_290_711L));
        } finally {
            try (Connection c = Database.open()) {
                Jdbc.update(c, "DELETE FROM SONG_MEDIA WHERE song_id=?", id);
                Jdbc.update(c, "DELETE FROM SONG WHERE song_id=?", id);
                Jdbc.update(c, "DELETE FROM MEMBER WHERE member_id=?", profile.id());
            }
            if (resource != null)
                Files.deleteIfExists(Path.of("config/audio", resource.substring(12)));
        }
    }
}
