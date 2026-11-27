package kr.ac.yumin.playlist;

import kr.ac.yumin.playlist.config.Database;
import kr.ac.yumin.playlist.dao.*;
import kr.ac.yumin.playlist.model.*;
import kr.ac.yumin.playlist.service.*;
import kr.ac.yumin.playlist.ui.*;

import java.awt.*;
import java.nio.file.*;
import java.util.concurrent.*;

import javax.swing.*;

/** Manual network check: real embed + real Oracle mock purchase; cleans only its own member. */
public class OnlineVideoSmoke {
    private static <T> T edt(Callable<T> job) throws Exception {
        FutureTask<T> task = new FutureTask<>(job);
        SwingUtilities.invokeAndWait(task);
        return task.get();
    }

    public static void main(String[] args) throws Exception {
        long id = args.length > 0 ? Long.parseLong(args[0]) : 1603382333L;
        Member member =
                new LocalProfileService()
                        .resolve(
                                Files.createTempDirectory(Path.of("target"), "online-smoke-")
                                        .resolve("profile.properties"));
        OnlineVideoPanel panel = null;
        boolean success = false;
        try {
            MediaTrack track;
            try (var c = Database.open()) {
                track = new MediaDao().find(c, id);
            }
            var link = new OnlineVideoService().resolve(id);
            final MediaTrack selected = track;
            MembershipService membership = new MembershipService(member);
            panel =
                    edt(
                            () -> {
                                Ui.theme();
                                var p = new OnlineVideoPanel();
                                p.mutedForTest(true);
                                JFrame f = new JFrame("muse · 온라인 재생 검증");
                                f.setContentPane(p);
                                f.setSize(1250, 850);
                                f.setLocationRelativeTo(null);
                                f.setVisible(true);
                                p.play(selected, link);
                                return p;
                            });
            final var p = panel;
            long until = System.currentTimeMillis() + 120000;
            while (System.currentTimeMillis() < until) {
                var s = p.state();
                if (s.present() && !s.paused() && s.duration() > 65 && s.position() > .5) break;
                Thread.sleep(250);
            }
            var playing = p.state();
            if (!playing.present() || playing.paused() || playing.duration() < 65)
                throw new IllegalStateException("공식 임베드 재생 실패: " + playing);
            if (args.length > 1 && args[1].equals("require-watch")) {
                long fullscreenUntil = System.currentTimeMillis() + 12000;
                while (System.currentTimeMillis() < fullscreenUntil && !p.watchFullscreen())
                    Thread.sleep(100);
                if (!p.watchMode() || !p.watchFullscreen())
                    throw new IllegalStateException("공식 웹 플레이어 전체화면 표시 실패");
                System.out.println("WATCH_FULLSCREEN_SUCCESS");
            }
            edt(
                    () -> {
                        p.seek(59);
                        return null;
                    });
            until = System.currentTimeMillis() + 12000;
            while (System.currentTimeMillis() < until && !p.state().minuteStopped())
                Thread.sleep(100);
            var stopped = p.state();
            if (!stopped.minuteStopped()
                    || !stopped.paused()
                    || stopped.position() < 59.8
                    || stopped.position() > 61)
                throw new IllegalStateException("무료 1분 정지 실패: " + stopped);
            System.out.println(
                    "FREE_STOP_SUCCESS song="
                            + id
                            + " duration="
                            + stopped.duration()
                            + " position="
                            + stopped.position());
            edt(
                    () -> {
                        p.togglePause();
                        return null;
                    });
            Thread.sleep(1000);
            if (!p.state().paused() || p.state().position() > 61)
                throw new IllegalStateException("무료 이어듣기 차단 실패: " + p.state());
            membership.purchase(java.util.UUID.randomUUID().toString());
            var paid = membership.status();
            if (!paid.active()) throw new IllegalStateException("Oracle 모의 결제 실패");
            edt(
                    () -> {
                        p.updatePass(paid);
                        p.togglePause();
                        return null;
                    });
            Thread.sleep(2300);
            if (p.state().paused() || p.state().position() < 61)
                throw new IllegalStateException("Plus 전체 재생 실패: " + p.state());
            System.out.println("PLUS_PLAY_SUCCESS position=" + p.state().position());
            membership.cancelLatest();
            var free = membership.status();
            edt(
                    () -> {
                        p.updatePass(free);
                        return null;
                    });
            Thread.sleep(700);
            if (!p.state().paused() || !p.state().minuteStopped())
                throw new IllegalStateException("취소 후 무료 제한 실패: " + p.state());
            System.out.println("CANCEL_LIMIT_SUCCESS position=" + p.state().position());
            new OnlineVideoService().confirmed(id, link.videoId());
            success = true;
        } finally {
            if (panel != null) {
                var p = panel;
                edt(
                        () -> {
                            p.close();
                            for (Window w : Window.getWindows()) w.dispose();
                            return null;
                        });
            }
            try (var c = Database.open()) {
                c.setAutoCommit(false);
                Jdbc.update(c, "DELETE FROM MEMBERSHIP_ORDER WHERE member_id=?", member.id());
                Jdbc.update(c, "DELETE FROM MEMBER WHERE member_id=?", member.id());
                c.commit();
            }
            System.out.println("SMOKE_RESULT=" + (success ? "PASS" : "FAIL"));
            System.exit(success ? 0 : 1);
        }
    }
}
