package kr.ac.yumin.playlist;

import static org.junit.jupiter.api.Assertions.*;

import kr.ac.yumin.playlist.audio.*;
import kr.ac.yumin.playlist.model.*;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.time.LocalDateTime;

import javax.sound.sampled.*;

class PlaybackTest {
    @Test
    void freePreviewCannotSeekBeyondSixtySeconds() {
        PlaybackPolicy p = new PlaybackPolicy(PassStatus.free());
        assertEquals(60, p.limit(276));
        assertEquals(60, p.clampSeek(120, 276));
        assertEquals(0, p.clampSeek(-10, 276));
        assertEquals(45, p.limit(45));
    }

    @Test
    void activePassUnlocksFullSongAndCancellationRestoresLimit() {
        LocalDateTime now = LocalDateTime.now();
        PlaybackPolicy p = new PlaybackPolicy(new PassStatus(now.plusDays(30), now));
        assertEquals(276, p.limit(276));
        assertEquals(120, p.clampSeek(120, 276));
        p.update(PassStatus.free());
        assertEquals(60, p.limit(276));
    }

    @Test
    void expiredPassDoesNotUnlockPlayback() {
        LocalDateTime now = LocalDateTime.now();
        PlaybackPolicy p = new PlaybackPolicy(new PassStatus(now.minusSeconds(1), now));
        assertEquals(60, p.limit(276));
    }

    @Test
    void allBundledRecordingsAreDecodableRealMp3Files() throws Exception {
        for (String name :
                new String[] {
                    "nocturne.mp3", "fantasie.mp3", "minute-waltz.mp3", "grande-valse.mp3"
                }) {
            try (InputStream file = getClass().getResourceAsStream("/audio/" + name)) {
                assertNotNull(file, name);
                try (AudioInputStream compressed =
                        AudioSystem.getAudioInputStream(new BufferedInputStream(file))) {
                    AudioFormat f = compressed.getFormat();
                    AudioFormat pcm =
                            new AudioFormat(
                                    AudioFormat.Encoding.PCM_SIGNED,
                                    f.getSampleRate(),
                                    16,
                                    f.getChannels(),
                                    f.getChannels() * 2,
                                    f.getSampleRate(),
                                    false);
                    try (AudioInputStream decoded =
                            AudioSystem.getAudioInputStream(pcm, compressed)) {
                        byte[] bytes =
                                decoded.readNBytes(
                                        (int) (pcm.getFrameRate() * pcm.getFrameSize() * 5));
                        assertTrue(bytes.length > 8192);
                        boolean nonzero = false;
                        for (byte b : bytes) nonzero |= b != 0;
                        assertTrue(nonzero, name);
                    }
                }
            }
        }
    }
}
