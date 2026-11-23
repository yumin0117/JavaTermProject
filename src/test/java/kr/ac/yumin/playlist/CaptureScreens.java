package kr.ac.yumin.playlist;

import kr.ac.yumin.playlist.config.Database;
import kr.ac.yumin.playlist.dao.Jdbc;
import kr.ac.yumin.playlist.model.*;
import kr.ac.yumin.playlist.service.*;
import kr.ac.yumin.playlist.ui.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.sql.*;
import java.util.List;
import java.util.concurrent.*;

import javax.imageio.ImageIO;
import javax.swing.*;

/** 실제 실행 중인 Swing 창을 캡처한다. UI 데이터는 실제 Oracle 서비스를 통해서만 채운다. */
public final class CaptureScreens {
    private static <T> T edt(Callable<T> task) throws Exception {
        FutureTask<T> f = new FutureTask<>(task);
        SwingUtilities.invokeAndWait(f);
        return f.get();
    }

    private static void settle(Window w) throws Exception {
        for (int i = 0; i < 120; i++) {
            Thread.sleep(150);
            if (edt(
                    () ->
                            !Boolean.TRUE.equals(
                                    ((RootPaneContainer) w)
                                            .getRootPane()
                                            .getClientProperty("busy")))) return;
        }
        throw new IllegalStateException("화면 로딩 시간 초과");
    }

    private static void capture(Window w, String name) throws Exception {
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

    private static List<Component> all(Container c) {
        var result = new java.util.ArrayList<Component>();
        for (Component child : c.getComponents()) {
            result.add(child);
            if (child instanceof Container d) result.addAll(all(d));
        }
        return result;
    }

    private static void click(Container c, String text) throws Exception {
        edt(
                () -> {
                    all(c).stream()
                            .filter(x -> x instanceof JButton b && b.getText().equals(text))
                            .map(x -> (JButton) x)
                            .findFirst()
                            .orElseThrow()
                            .doClick();
                    return null;
                });
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of("docs/screenshots"));
        AuthFrame login =
                edt(
                        () -> {
                            Ui.theme();
                            AuthFrame f = new AuthFrame();
                            f.setVisible(true);
                            return f;
                        });
        capture(login, "01_login");
        SwingUtilities.invokeLater(
                () ->
                        all(login).stream()
                                .filter(
                                        x ->
                                                x instanceof JButton b
                                                        && b.getText().equals("새 계정 만들기"))
                                .map(x -> (JButton) x)
                                .findFirst()
                                .orElseThrow()
                                .doClick());
        Thread.sleep(400);
        Window signup =
                edt(
                        () ->
                                java.util.Arrays.stream(Window.getWindows())
                                        .filter(w -> w instanceof JDialog && w.isVisible())
                                        .findFirst()
                                        .orElseThrow());
        capture(signup, "02_signup");
        edt(
                () -> {
                    signup.dispose();
                    login.dispose();
                    return null;
                });
        if (args.length > 0 && args[0].equals("login-only")) {
            System.exit(0);
            return;
        }
        AuthService auth = new AuthService();
        String user = "report_" + Long.toUnsignedString(System.nanoTime(), 36);
        long id = 0;
        try {
            char[] password = java.util.UUID.randomUUID().toString().toCharArray();
            char[] confirmation = password.clone(), loginPassword = password.clone();
            auth.register(user, password, confirmation, "김유민", "report@example.com");
            Member member = auth.login(user, loginPassword);
            PlaylistService service = new PlaylistService(member);
            id = service.create("산책길의 작은 음악 모음", "조용히 걷는 시간에 어울리는 곡을 모았습니다.", false);
            service.add(id, 101);
            service.add(id, 103);
            service.add(id, 102);
            MainFrame main =
                    edt(
                            () -> {
                                MainFrame f = new MainFrame(member);
                                f.setVisible(true);
                                return f;
                            });
            settle(main);
            capture(main, "03_my_playlists");
            long playlist = id;
            DetailDialog detail =
                    edt(
                            () -> {
                                DetailDialog d =
                                        new DetailDialog(main, service, playlist, main::refresh);
                                d.setModalityType(Dialog.ModalityType.MODELESS);
                                d.setVisible(true);
                                return d;
                            });
            settle(detail);
            edt(
                    () -> {
                        all(detail).stream()
                                .filter(x -> x instanceof JTextField)
                                .map(x -> (JTextField) x)
                                .findFirst()
                                .orElseThrow()
                                .setText("네온");
                        return null;
                    });
            click(detail, "곡 검색");
            settle(detail);
            capture(detail, "04_song_search");
            service.move(id, 102, -1);
            edt(
                    () -> {
                        detail.reload();
                        return null;
                    });
            settle(detail);
            capture(detail, "05_reordered");
            service.edit(id, "산책길의 작은 음악 모음", "공개한 목록은 다른 회원도 조회하고 좋아요를 누를 수 있습니다.", true);
            service.like(id, true);
            edt(
                    () -> {
                        detail.dispose();
                        main.refresh();
                        return null;
                    });
            settle(main);
            edt(
                    () -> {
                        all(main).stream()
                                .filter(x -> x instanceof JTabbedPane)
                                .map(x -> (JTabbedPane) x)
                                .findFirst()
                                .orElseThrow()
                                .setSelectedIndex(1);
                        return null;
                    });
            capture(main, "06_public_likes");
            DetailDialog shared =
                    edt(
                            () -> {
                                DetailDialog d =
                                        new DetailDialog(main, service, 1001, main::refresh);
                                d.setModalityType(Dialog.ModalityType.MODELESS);
                                d.setVisible(true);
                                return d;
                            });
            settle(shared);
            capture(shared, "07_other_public");
            edt(
                    () -> {
                        shared.dispose();
                        return null;
                    });
            service.remove(id, 101);
            service.delete(id);
            id = 0;
            edt(
                    () -> {
                        all(main).stream()
                                .filter(x -> x instanceof JTabbedPane)
                                .map(x -> (JTabbedPane) x)
                                .findFirst()
                                .orElseThrow()
                                .setSelectedIndex(0);
                        main.refresh();
                        return null;
                    });
            settle(main);
            capture(main, "08_deleted");
            edt(
                    () -> {
                        main.dispose();
                        return null;
                    });
        } finally {
            try (Connection c = Database.open()) {
                if (id != 0)
                    Jdbc.update(
                            c,
                            "DELETE FROM PLAYLIST WHERE playlist_id=? AND member_id=?",
                            id,
                            user);
                Jdbc.update(c, "DELETE FROM MEMBER WHERE member_id=?", user);
            }
        }
        System.out.println("Oracle 화면 캡처 완료. 임시 회원과 목록 정리 완료.");
        System.exit(0);
    }
}
