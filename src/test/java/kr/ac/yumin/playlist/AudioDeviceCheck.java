package kr.ac.yumin.playlist;

import kr.ac.yumin.playlist.audio.*;
import kr.ac.yumin.playlist.model.*;

import java.time.LocalDateTime;

/** 수동 실행: 실제 기본 사운드 장치를 열되 음량 0으로 경계 프레임을 검증한다. */
public final class AudioDeviceCheck {
    public static void main(String[] args) throws Exception {
        var track =
                new MediaTrack(
                        new Song(
                                9001,
                                "녹턴 2번",
                                "Frédéric Chopin",
                                "Nocturnes, Op. 9",
                                "클래식",
                                277,
                                0),
                        "audio/nocturne.mp3",
                        "Musopen",
                        "https://archive.org/details/musopen-chopin",
                        "CC0 1.0");
        try (AudioPlayer p = new AudioPlayer()) {
            p.volume(0);
            p.play(track, PassStatus.free());
            long until = System.currentTimeMillis() + 10000;
            while (p.position() < .1 && p.playing() && System.currentTimeMillis() < until)
                Thread.sleep(50);
            if (p.error() != null) throw new IllegalStateException(p.error());
            if (p.position() < .1) throw new IllegalStateException("사운드 장치 시작 시간 초과");
            p.seek(59);
            until = System.currentTimeMillis() + 10000;
            while (p.playing() && System.currentTimeMillis() < until) Thread.sleep(50);
            if (!p.limited() || p.position() > 60.01 || p.position() < 59.9)
                throw new IllegalStateException("무료 재생 경계 오류: " + p.position());
            LocalDateTime now = LocalDateTime.now();
            p.play(track, new PassStatus(now.plusDays(30), now));
            Thread.sleep(300);
            p.seek(61);
            until = System.currentTimeMillis() + 10000;
            while (p.position() < 61.1 && p.playing() && System.currentTimeMillis() < until)
                Thread.sleep(50);
            if (p.position() < 61.1)
                throw new IllegalStateException("전체 재생 검증 실패: " + p.position());
            p.togglePause();
            Thread.sleep(250);
            double before = p.position();
            Thread.sleep(300);
            if (Math.abs(p.position() - before) > .15) throw new IllegalStateException("일시정지 실패");
            p.togglePause();
            Thread.sleep(300);
            System.out.println("실제 오디오 장치: 무료 60초 정지, 유료 61초 이후 재생, 일시정지·재개 확인 (음량 0)");
        }
    }
}
