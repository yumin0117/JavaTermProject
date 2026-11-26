package kr.ac.yumin.playlist.model;

import java.time.*;
import java.util.List;

public record AlbumDetail(
        MediaTrack track,
        long albumId,
        String title,
        String artists,
        LocalDate releaseDate,
        String type,
        String genre,
        String distributor,
        String agency,
        String coverUrl,
        String sourceUrl,
        LocalDateTime fetchedAt,
        List<Credit> credits,
        List<Song> songs) {
    public record Credit(String role, int order, String name) {}

    public String people(String role) {
        String value =
                credits.stream()
                        .filter(c -> c.role().equals(role))
                        .map(Credit::name)
                        .distinct()
                        .collect(java.util.stream.Collectors.joining(", "));
        return value.isBlank() ? "제공된 정보 없음" : value;
    }
}
