package kr.ac.yumin.playlist.ui;

import kr.ac.yumin.playlist.model.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.*;

/** 디자인 QA 전용 픽스처. 운영 앱은 이 데이터를 사용하지 않으며 저장 동작은 실행하지 않는다. */
public final class DesignCapture {
    private static void field(Object o, String key, Object value) throws Exception {
        Field f = o.getClass().getDeclaredField(key);
        f.setAccessible(true);
        f.set(o, value);
    }

    private static void call(Object o, String method) throws Exception {
        Method m = o.getClass().getDeclaredMethod(method);
        m.setAccessible(true);
        m.invoke(o);
    }

    private static void save(MainFrame frame, String name) throws Exception {
        JComponent content = (JComponent) frame.getContentPane();
        BufferedImage image =
                new BufferedImage(
                        content.getWidth(), content.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        content.printAll(g);
        g.dispose();
        ImageIO.write(image, "png", Path.of("docs/screenshots/" + name + ".png").toFile());
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of("docs/screenshots"));
        MainFrame[] holder = new MainFrame[1];
        SwingUtilities.invokeAndWait(
                () -> {
                    Ui.theme();
                    holder[0] = new MainFrame();
                    holder[0].setVisible(true);
                });
        Thread.sleep(700);
        SwingUtilities.invokeAndWait(
                () -> {
                    try {
                        MainFrame f = holder[0];
                        save(f, "10_home_disconnected");
                        if (args.length > 0 && args[0].equals("preview")) {
                            var names =
                                    new String[] {
                                        "녹턴 2번, Op. 9 No. 2",
                                        "환상 즉흥곡, Op. 66",
                                        "강아지 왈츠, Op. 64 No. 1",
                                        "화려한 대왈츠, Op. 18"
                                    };
                            var albums =
                                    new String[] {
                                        "Nocturnes, Op. 9",
                                        "Fantaisie-Impromptu, Op. 66",
                                        "Waltzes, Op. 64",
                                        "Grande valse brillante, Op. 18"
                                    };
                            var files =
                                    new String[] {
                                        "nocturne.mp3",
                                        "fantasie.mp3",
                                        "minute-waltz.mp3",
                                        "grande-valse.mp3"
                                    };
                            int[] duration = {277, 326, 108, 320};
                            var rows = new java.util.ArrayList<MediaTrack>();
                            for (int i = 0; i < 4; i++)
                                rows.add(
                                        new MediaTrack(
                                                new Song(
                                                        9001 + i,
                                                        names[i],
                                                        "Frédéric Chopin",
                                                        albums[i],
                                                        "클래식",
                                                        duration[i],
                                                        0),
                                                "audio/" + files[i],
                                                "Musopen",
                                                "https://archive.org/details/musopen-chopin",
                                                "CC0 1.0"));
                            field(f, "tracks", List.copyOf(rows));
                            Field label = MainFrame.class.getDeclaredField("connection");
                            label.setAccessible(true);
                            ((JLabel) label.get(f))
                                    .setText("디자인 확인용 화면 · 실제 저장·재생은 Oracle 연결 후 사용");
                            call(f, "render");
                            f.validate();
                            save(f, "11_home_design_preview");
                            field(f, "page", "전체 곡");
                            call(f, "render");
                            f.validate();
                            save(f, "12_library_design_preview");
                        }
                        f.dispose();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
        System.exit(0);
    }
}
