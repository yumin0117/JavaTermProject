package kr.ac.yumin.playlist;

import static org.junit.jupiter.api.Assertions.*;

import kr.ac.yumin.playlist.config.Database;
import kr.ac.yumin.playlist.dao.Jdbc;
import kr.ac.yumin.playlist.service.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

@Tag("oracle")
class MusicOracleTest {
    @TempDir Path folder;

    @Test
    void automaticProfileRealCatalogAndTransactionalDemoPayment() throws Exception {
        LocalProfileService profiles = new LocalProfileService();
        var user = profiles.resolve(folder.resolve("profile.properties"));
        try {
            assertEquals(user.id(), profiles.resolve(folder.resolve("profile.properties")).id());
            MusicLibraryService music = new MusicLibraryService(user);
            MembershipService membership = new MembershipService(user);
            assertTrue(music.catalog().size() >= 100);
            assertEquals(1, music.catalog().get(0).rank());
            assertEquals(100, music.catalog().get(99).rank());
            assertFalse(membership.status().active());
            assertEquals(60, music.authorize(9001).pass().limitSeconds(277));
            assertThrows(AppException.class, () -> music.authorize(101));
            String id = UUID.randomUUID().toString();
            ExecutorService pool = Executors.newFixedThreadPool(2);
            try {
                Callable<Boolean> pay =
                        () -> {
                            membership.purchase(id);
                            return true;
                        };
                for (var result : pool.invokeAll(List.of(pay, pay))) assertTrue(result.get());
            } finally {
                pool.shutdownNow();
            }
            assertEquals(1, membership.history().size());
            assertTrue(membership.status().active());
            assertEquals(277, music.authorize(9001).pass().limitSeconds(277));
            assertEquals(4900, membership.history().get(0).amount());
            membership.cancelLatest();
            assertFalse(membership.status().active());
            assertEquals("CANCELLED", membership.history().get(0).status());
            assertEquals(60, music.authorize(9001).pass().limitSeconds(277));
            membership.purchase(UUID.randomUUID().toString());
            assertTrue(membership.status().active());
            assertEquals(2, membership.history().size());
        } finally {
            try (Connection c = Database.open()) {
                Jdbc.update(c, "DELETE FROM MEMBERSHIP_ORDER WHERE member_id=?", user.id());
                Jdbc.update(c, "DELETE FROM MEMBER WHERE member_id=?", user.id());
            }
        }
    }
}
