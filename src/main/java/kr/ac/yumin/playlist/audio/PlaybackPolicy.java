package kr.ac.yumin.playlist.audio;

import kr.ac.yumin.playlist.model.PassStatus;

import java.time.Duration;

/** DB 시각과 만료일의 차이를 단조 시계로 바꾸므로 재생 중 만료도 반영한다. */
public final class PlaybackPolicy {
    public static final int FREE_SECONDS = 60;
    private volatile long premiumUntilNanos;

    public PlaybackPolicy(PassStatus status) {
        update(status);
    }

    public void update(PassStatus status) {
        long remaining =
                status.active()
                        ? Duration.between(status.databaseNow(), status.expiresAt()).toNanos()
                        : 0;
        premiumUntilNanos = System.nanoTime() + remaining;
    }

    public int limit(int duration) {
        return System.nanoTime() < premiumUntilNanos ? duration : Math.min(FREE_SECONDS, duration);
    }

    public double clampSeek(double seconds, int duration) {
        return Math.max(0, Math.min(seconds, limit(duration)));
    }
}
