package kr.ac.yumin.playlist.model;

public record Song(
        long id, String title, String artists, String album, String genre, int seconds, int order) {
    public String duration() {
        if (seconds <= 0) return "—";
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }
}
