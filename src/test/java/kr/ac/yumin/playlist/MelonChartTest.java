package kr.ac.yumin.playlist;

import static org.junit.jupiter.api.Assertions.*;

import kr.ac.yumin.playlist.service.*;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

class MelonChartTest {
    private String html() throws Exception {
        try (var in = getClass().getResourceAsStream("/melon-top100.html")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void parsesRealSnapshotAndStableExternalIds() throws Exception {
        var snapshot = new MelonChartService().parse(Jsoup.parse(html(), MelonChartService.SOURCE));
        assertEquals(LocalDateTime.of(2026, 11, 26, 11, 0), snapshot.chartAt());
        assertEquals(100, snapshot.entries().size());
        assertEquals("꿈꾸던 어른이 되었나요?", snapshot.entries().get(0).title());
        assertEquals(1_602_290_711L, snapshot.entries().get(0).songId());
        assertEquals(2, snapshot.entries().get(2).artists().size());
        assertEquals(100, snapshot.entries().get(99).rank());
    }

    @Test
    void rejectsPartialOrDuplicateRankWithoutTouchingDatabase() throws Exception {
        var partial = Jsoup.parse(html());
        partial.select("tr").last().remove();
        assertThrows(AppException.class, () -> new MelonChartService().parse(partial));
        var duplicate = Jsoup.parse(html());
        duplicate.select(".rank").get(1).text("1");
        assertThrows(AppException.class, () -> new MelonChartService().parse(duplicate));
    }
}
