package kr.ac.yumin.playlist;

import static org.junit.jupiter.api.Assertions.*;

import kr.ac.yumin.playlist.model.OnlineVideo;
import kr.ac.yumin.playlist.service.OnlineVideoService;

import org.junit.jupiter.api.Test;

class OnlineVideoTest {
    @Test
    void acceptsOnlyHttpsYoutubeWatchUrlsWithExactVideoIds() {
        assertEquals(
                "D968qfUZMUE",
                OnlineVideoService.videoId("https://www.youtube.com/watch?v=D968qfUZMUE&list=abc"));
        for (String url :
                new String[] {
                    "https://www.youtube.com.evil.example/watch?v=D968qfUZMUE",
                    "http://www.youtube.com/watch?v=D968qfUZMUE",
                    "file:///watch?v=D968qfUZMUE",
                    "https://www.youtube.com/watch?v=too-short",
                    "https://www.youtube.com/redirect?v=D968qfUZMUE",
                    "https://user@evil.example/watch?v=D968qfUZMUE"
                }) assertNull(OnlineVideoService.videoId(url));
    }

    @Test
    void missingVideoRemainsSearchMetadataRatherThanInventingPlayback() {
        var link = new OnlineVideo(1, null, null, null, "SEARCH", "곡 & 가수", "SEARCH");
        assertFalse(link.hasVideo());
        assertTrue(link.searchUrl().contains("%26"));
        var ready =
                new OnlineVideo(1, "D968qfUZMUE", "NEON", "A2O WORLD", "MV", "NEON", "METADATA");
        assertTrue(ready.hasVideo());
        assertEquals("공식 뮤직비디오", ready.kindLabel());
    }
}
