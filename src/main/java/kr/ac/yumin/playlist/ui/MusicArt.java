package kr.ac.yumin.playlist.ui;

import java.awt.*;
import java.awt.geom.*;

import javax.swing.*;

/** 외부 앨범 표지를 복제하지 않는 코드 기반 커버 그래픽. */
public final class MusicArt extends JComponent implements Icon {
    private final int variant, size;
    private static final Color[][] PALETTES = {
        {new Color(36, 62, 75), new Color(124, 168, 172)},
        {new Color(205, 108, 84), new Color(245, 203, 157)},
        {new Color(107, 101, 161), new Color(214, 195, 231)},
        {new Color(69, 116, 98), new Color(190, 211, 165)}
    };

    public MusicArt(long key, int size) {
        variant = Math.floorMod((int) key - 9001, 4);
        this.size = size;
        setPreferredSize(new Dimension(size, size));
        setMinimumSize(new Dimension(48, 48));
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D copy = (Graphics2D) g.create();
        copy.translate(x, y);
        draw(copy, size, size);
        copy.dispose();
    }

    @Override
    protected void paintComponent(Graphics g) {
        draw((Graphics2D) g, getWidth(), getHeight());
    }

    private void draw(Graphics2D source, int width, int height) {
        Graphics2D g = (Graphics2D) source.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.clip(new RoundRectangle2D.Float(0, 0, width, height, 18, 18));
        Color[] colors = PALETTES[variant];
        g.setPaint(new GradientPaint(0, 0, colors[0], width, height, colors[1]));
        g.fillRect(0, 0, width, height);
        double scale = Math.min(width, height) / 200.0;
        g.scale(width / 200.0, height / 200.0);
        g.setColor(new Color(255, 255, 255, 32));
        g.setStroke(new BasicStroke(1.2f));
        for (int i = 0; i < 12; i++) {
            double r = 45 + i * 14;
            g.draw(new Ellipse2D.Double(105 - r, 70 - r, r * 2, r * 2));
        }
        g.setColor(new Color(255, 248, 229, 185));
        if (variant == 0) {
            g.fill(new Ellipse2D.Double(103, 33, 54, 54));
            g.setColor(colors[0]);
            g.fill(new Ellipse2D.Double(121, 26, 51, 51));
        } else if (variant == 1) {
            g.rotate(-.45, 110, 100);
            for (int i = 0; i < 6; i++) {
                g.setColor(new Color(255, 244, 222, 45 + i * 22));
                g.fillRoundRect(30 + i * 24, 40, 12, 170, 10, 10);
            }
            g.rotate(.45, 110, 100);
        } else if (variant == 2) {
            for (int i = 0; i < 4; i++)
                g.fill(new Arc2D.Double(28 + i * 31, 35 + i * 18, 90, 90, 20, 270, Arc2D.OPEN));
        } else {
            g.fill(new Ellipse2D.Double(40, 35, 105, 105));
            g.setColor(new Color(46, 88, 76, 180));
            for (int i = 0; i < 9; i++) g.fillRoundRect(12, 105 + i * 8, 180, 3, 3, 3);
        }
        if (width > 95 && height > 95) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g.drawString(
                    new String[] {"MUSE", "MUSIC", "PLAYLIST", "LIBRARY"}[variant], 18, 173);
            g.setFont(new Font("Segoe UI", Font.PLAIN, 8));
            g.drawString("YOUR MUSIC COLLECTION", 18, 187);
        }
        g.dispose();
    }
}
