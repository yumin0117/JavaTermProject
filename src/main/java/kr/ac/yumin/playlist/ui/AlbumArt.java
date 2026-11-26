package kr.ac.yumin.playlist.ui;

import kr.ac.yumin.playlist.model.MediaTrack;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import javax.imageio.ImageIO;
import javax.swing.*;

/** Album thumbnails load off the EDT; missing images use the app's own artwork. */
final class AlbumArt implements Icon {
    private static final Map<String, BufferedImage> CACHE = new ConcurrentHashMap<>();
    private static final Set<String> PENDING = ConcurrentHashMap.newKeySet();
    private static final ExecutorService FETCH =
            Executors.newFixedThreadPool(
                    3,
                    r -> {
                        Thread t = new Thread(r, "album-art");
                        t.setDaemon(true);
                        return t;
                    });
    private final MediaTrack track;
    private final int size;

    AlbumArt(MediaTrack track, int size) {
        this.track = track;
        this.size = size;
    }

    public int getIconWidth() {
        return size;
    }

    public int getIconHeight() {
        return size;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        String url = track.coverUrl();
        BufferedImage image = url == null ? null : CACHE.get(url);
        if (image != null) g.drawImage(image, x, y, size, size, null);
        else {
            new MusicArt(track.song().id(), size).paintIcon(c, g, x, y);
            if (url != null && PENDING.add(url))
                FETCH.submit(
                        () -> {
                            try {
                                URI uri = URI.create(url);
                                if (!"https".equals(uri.getScheme())
                                        || !"cdnimg.melon.co.kr".equals(uri.getHost())) return;
                                URLConnection connection = uri.toURL().openConnection();
                                connection.setConnectTimeout(6000);
                                connection.setReadTimeout(6000);
                                try (var input = connection.getInputStream()) {
                                    BufferedImage loaded = ImageIO.read(input);
                                    if (loaded != null) CACHE.put(url, loaded);
                                }
                            } catch (Exception ignored) {
                            } finally {
                                SwingUtilities.invokeLater(c::repaint);
                            }
                        });
        }
    }
}
