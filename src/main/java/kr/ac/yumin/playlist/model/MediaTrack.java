package kr.ac.yumin.playlist.model;

public record MediaTrack(
        Song song,
        String resource,
        String performer,
        String sourceUrl,
        String license,
        int rank,
        String coverUrl,
        String chartAt) {
    public MediaTrack(
            Song song, String resource, String performer, String sourceUrl, String license) {
        this(song, resource, performer, sourceUrl, license, 0, null, null);
    }

    public boolean playable() {
        return resource != null && !resource.isBlank();
    }
}
