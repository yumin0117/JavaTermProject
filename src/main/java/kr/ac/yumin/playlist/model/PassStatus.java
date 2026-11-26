package kr.ac.yumin.playlist.model;

import java.time.LocalDateTime;

public record PassStatus(LocalDateTime expiresAt, LocalDateTime databaseNow) {
    public boolean active() {
        return expiresAt != null && expiresAt.isAfter(databaseNow);
    }

    public int limitSeconds(int duration) {
        return active() ? duration : Math.min(60, duration);
    }

    public static PassStatus free() {
        return new PassStatus(null, LocalDateTime.now());
    }
}
