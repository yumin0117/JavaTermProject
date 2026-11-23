package kr.ac.yumin.playlist.model;

public record Song(
        long id, String title, String artists, String album, String genre, int seconds, int order) {
    public String duration() {
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }
}
