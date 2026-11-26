package kr.ac.yumin.playlist;

import kr.ac.yumin.playlist.audio.AudioPlayer;
import kr.ac.yumin.playlist.config.Database;
import kr.ac.yumin.playlist.dao.Jdbc;
import kr.ac.yumin.playlist.model.*;
import kr.ac.yumin.playlist.service.*;
import kr.ac.yumin.playlist.ui.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.sql.*;
import java.util.concurrent.*;

import javax.imageio.ImageIO;
import javax.swing.*;

/** 실제 Oracle 데이터로 실행하는 화면 캡처. 새 임시 계정의 데이터만 정리한다. */
public final class CaptureScreens {
    private static <T> T edt(Callable<T> task) throws Exception {
        FutureTask<T> f = new FutureTask<>(task);
        SwingUtilities.invokeAndWait(f);
        return f.get();
    }

    private static Object field(Object o, String name) throws Exception {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }

    private static void invoke(Object o, String name, Class<?>[] types, Object... args)
            throws Exception {
        Method m = o.getClass().getDeclaredMethod(name, types);
        m.setAccessible(true);
        m.invoke(o, args);
    }

    private static void settle(Window w) throws Exception {
        for (int i = 0; i < 200; i++) {
            Thread.sleep(100);
            boolean busy =
                    edt(
                            () -> {
                                if (Boolean.TRUE.equals(
                                        ((RootPaneContainer) w)
                                                .getRootPane()
                                                .getClientProperty("busy"))) return true;
                                return w instanceof MainFrame && (boolean) field(w, "loading");
                            });
            if (!busy) {
                edt(
                        () -> {
                            w.validate();
                            return null;
                        });
                return;
            }
        }
        throw new IllegalStateException("화면 로딩 시간 초과");
    }

    private static void capture(Window w, String name) throws Exception {
        settle(w);
        edt(
                () -> {
                    JComponent content = (JComponent) ((RootPaneContainer) w).getContentPane();
                    BufferedImage image =
                            new BufferedImage(
                                    content.getWidth(),
                                    content.getHeight(),
                                    BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = image.createGraphics();
                    content.printAll(g);
                    g.dispose();
                    ImageIO.write(
                            image, "png", Path.of("docs/screenshots/" + name + ".png").toFile());
                    return null;
                });
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of("docs/screenshots"));
        Path temporary = Files.createTempDirectory(Path.of("target"), "capture-");
        Member member = new LocalProfileService().resolve(temporary.resolve("profile.properties"));
        PlaylistService service = new PlaylistService(member);
        MembershipService membership = new MembershipService(member);
        MusicLibraryService music = new MusicLibraryService(member);
        MainFrame main = null;
        long id = 0;
        var chart = music.catalog();
        long first = chart.get(0).song().id(),
                second = chart.get(1).song().id(),
                third = chart.get(2).song().id();
        try {
            main =
                    edt(
                            () -> {
                                Ui.theme();
                                MainFrame f = new MainFrame(member);
                                f.setVisible(true);
                                return f;
                            });
            MainFrame f = main;
            settle(f);
            if (edt(() -> field(f, "library")) == null)
                throw new IllegalStateException("실제 Oracle 보관함 연결 실패");
            edt(
                    () -> {
                        ((AudioPlayer) field(f, "player")).volume(0);
                        return null;
                    });
            Thread.sleep(3000);
            capture(f, "13_home_live");
            edt(
                    () -> {
                        invoke(f, "navigate", new Class<?>[] {String.class}, "전체 곡");
                        return null;
                    });
            capture(f, "14_library_live");
            id = service.create("오늘의 TOP100", "멜론 차트에서 발견한 나만의 음악.", true);
            service.add(id, first);
            service.add(id, second);
            service.add(id, third);
            service.like(id, true);
            edt(
                    () -> {
                        invoke(f, "navigate", new Class<?>[] {String.class}, "내 플레이리스트");
                        f.refresh();
                        return null;
                    });
            capture(f, "15_my_playlist_live");
            long playlist = id;
            DetailDialog detail =
                    edt(
                            () -> {
                                DetailDialog d =
                                        new DetailDialog(
                                                f, service, playlist, f::refresh, f::playSong);
                                d.setModalityType(Dialog.ModalityType.MODELESS);
                                d.setVisible(true);
                                return d;
                            });
            capture(detail, "16_playlist_live");
            service.move(id, third, -1);
            edt(
                    () -> {
                        detail.reload();
                        return null;
                    });
            capture(detail, "17_reorder_live");
            edt(
                    () -> {
                        detail.dispose();
                        return null;
                    });
            MembershipDialog free =
                    edt(
                            () -> {
                                MembershipDialog d =
                                        new MembershipDialog(f, membership, f::refresh);
                                d.setModalityType(Dialog.ModalityType.MODELESS);
                                d.setVisible(true);
                                return d;
                            });
            capture(free, "18_free_pass_live");
            edt(
                    () -> {
                        free.dispose();
                        return null;
                    });
            membership.purchase(java.util.UUID.randomUUID().toString());
            MembershipDialog paid =
                    edt(
                            () -> {
                                MembershipDialog d =
                                        new MembershipDialog(f, membership, f::refresh);
                                d.setModalityType(Dialog.ModalityType.MODELESS);
                                d.setVisible(true);
                                return d;
                            });
            capture(paid, "19_paid_pass_live");
            edt(
                    () -> {
                        paid.dispose();
                        f.refresh();
                        return null;
                    });
            settle(f);
            edt(
                    () -> {
                        invoke(f, "navigate", new Class<?>[] {String.class}, "전체 곡");
                        invoke(f, "select", new Class<?>[] {MediaTrack.class}, chart.get(0));
                        return null;
                    });
            capture(f, "20_chart_selection_live");
            membership.cancelLatest();
            MembershipDialog cancelled =
                    edt(
                            () -> {
                                MembershipDialog d =
                                        new MembershipDialog(f, membership, f::refresh);
                                d.setModalityType(Dialog.ModalityType.MODELESS);
                                d.setVisible(true);
                                return d;
                            });
            capture(cancelled, "21_cancelled_pass_live");
            edt(
                    () -> {
                        cancelled.dispose();
                        return null;
                    });
            service.remove(id, second);
            service.delete(id);
            id = 0;
            edt(
                    () -> {
                        invoke(f, "navigate", new Class<?>[] {String.class}, "내 플레이리스트");
                        f.refresh();
                        return null;
                    });
            capture(f, "22_deleted_live");
            edt(
                    () -> {
                        f.dispose();
                        return null;
                    });
            System.out.println("실제 Oracle TOP100 화면 10개 캡처 완료 (음원 연결 전 차트 정보 표시)");
        } finally {
            if (main != null) {
                MainFrame f = main;
                edt(
                        () -> {
                            f.dispose();
                            return null;
                        });
            }
            try (Connection c = Database.open()) {
                if (id != 0)
                    Jdbc.update(
                            c,
                            "DELETE FROM PLAYLIST WHERE playlist_id=? AND member_id=?",
                            id,
                            member.id());
                Jdbc.update(c, "DELETE FROM MEMBERSHIP_ORDER WHERE member_id=?", member.id());
                Jdbc.update(c, "DELETE FROM MEMBER WHERE member_id=?", member.id());
            }
        }
        System.exit(0);
    }
}
