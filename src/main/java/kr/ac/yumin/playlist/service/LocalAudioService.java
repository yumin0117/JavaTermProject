package kr.ac.yumin.playlist.service;

import kr.ac.yumin.playlist.config.Database;
import kr.ac.yumin.playlist.dao.Jdbc;

import java.nio.file.*;
import java.sql.*;
import java.util.UUID;

import javax.sound.sampled.*;

/** User-owned MP3 files stay outside the public repository. */
public final class LocalAudioService {
    public void attach(long songId, Path source) throws Exception {
        if (!source.getFileName().toString().toLowerCase().endsWith(".mp3")
                || Files.size(source) > 100_000_000)
            throw new AppException("100MB 이하의 MP3 파일을 선택해 주세요.");
        Path folder = Path.of("config/audio");
        Files.createDirectories(folder);
        String name = UUID.randomUUID() + ".mp3";
        Path target = folder.resolve(name);
        Files.copy(source, target);
        boolean saved = false;
        try {
            int seconds = duration(target);
            if (seconds <= 0) throw new AppException("재생 길이를 확인할 수 없는 음원입니다.");
            try (Connection c = Database.open()) {
                c.setAutoCommit(false);
                try {
                    if (Jdbc.list(
                                    c,
                                    "SELECT song_id FROM SONG WHERE song_id=? FOR UPDATE WAIT 5",
                                    r -> r.getLong(1),
                                    songId)
                            .isEmpty()) throw new AppException("등록된 곡을 찾을 수 없습니다.");
                    Jdbc.update(
                            c,
                            "MERGE INTO SONG_MEDIA d USING(SELECT ? id FROM DUAL) x"
                                + " ON(d.song_id=x.id) WHEN MATCHED THEN UPDATE SET"
                                + " resource_path=?,performer='사용자 연결"
                                + " 음원',source_url='local:private',license_name='사용자 보유 음원' WHEN"
                                + " NOT MATCHED THEN"
                                + " INSERT(song_id,resource_path,performer,source_url,license_name)"
                                + " VALUES(x.id,?,'사용자 연결 음원','local:private','사용자 보유 음원')",
                            songId,
                            "local-audio/" + name,
                            "local-audio/" + name);
                    Jdbc.update(
                            c,
                            "UPDATE SONG SET duration_seconds=? WHERE song_id=?",
                            seconds,
                            songId);
                    c.commit();
                    saved = true;
                } catch (Exception e) {
                    c.rollback();
                    throw e;
                }
            }
        } finally {
            if (!saved) Files.deleteIfExists(target);
        }
    }

    private int duration(Path path) throws Exception {
        try (AudioInputStream input = AudioSystem.getAudioInputStream(path.toFile())) {
            AudioFormat f = input.getFormat();
            AudioFormat pcm =
                    new AudioFormat(
                            AudioFormat.Encoding.PCM_SIGNED,
                            f.getSampleRate(),
                            16,
                            f.getChannels(),
                            f.getChannels() * 2,
                            f.getSampleRate(),
                            false);
            try (AudioInputStream decoded = AudioSystem.getAudioInputStream(pcm, input)) {
                byte[] buffer = new byte[32768];
                long bytes = 0;
                int n;
                while ((n = decoded.read(buffer)) >= 0) bytes += n;
                return (int) Math.ceil(bytes / (pcm.getFrameSize() * pcm.getFrameRate()));
            }
        }
    }
}
