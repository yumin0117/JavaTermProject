package kr.ac.yumin.playlist.service;

import kr.ac.yumin.playlist.config.Database;
import kr.ac.yumin.playlist.dao.MediaDao;
import kr.ac.yumin.playlist.model.*;

import java.sql.*;
import java.util.*;

public final class MusicLibraryService {
    private final MediaDao media = new MediaDao();
    private final MembershipService membership;

    public MusicLibraryService(Member member) {
        membership = new MembershipService(member);
    }

    public List<MediaTrack> catalog() throws SQLException {
        try (Connection c = Database.open()) {
            return media.catalog(c);
        }
    }

    public record Playback(MediaTrack track, PassStatus pass) {}

    public Playback authorize(long songId) throws SQLException {
        MediaTrack track;
        try (Connection c = Database.open()) {
            track = media.find(c, songId);
        }
        if (track == null || !track.playable())
            throw new AppException("곡 정보만 등록되어 있습니다. 재생하려면 사용 권한이 있는 MP3 음원을 연결해 주세요.");
        if (!track.resource().matches("audio/[a-z0-9-]+[.]mp3|local-audio/[a-f0-9-]+[.]mp3"))
            throw new AppException("음원 경로가 올바르지 않습니다.");
        return new Playback(track, membership.status());
    }
}
