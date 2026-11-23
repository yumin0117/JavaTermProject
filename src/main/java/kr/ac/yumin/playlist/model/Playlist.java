package kr.ac.yumin.playlist.model;

import java.time.LocalDateTime;

public record Playlist(
        long id,
        String title,
        String description,
        boolean isPublic,
        String owner,
        String nickname,
        LocalDateTime createdAt,
        int likes,
        boolean liked) {}
