package kr.ac.yumin.playlist.ui;

import java.awt.*;
import java.awt.geom.*;

import javax.swing.*;

public final class MuseIcon implements Icon {
    private final String name;
    private final int size;
    private final Color color;

    public MuseIcon(String name, int size, Color color) {
        this.name = name;
        this.size = size;
        this.color = color;
    }

    public int getIconWidth() {
        return size;
    }

    public int getIconHeight() {
        return size;
    }

    public void paintIcon(Component c, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.translate(x, y);
        g.scale(size / 24.0, size / 24.0);
        g.setColor(color);
        g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (name) {
            case "play" -> {
                Path2D p = new Path2D.Float();
                p.moveTo(8, 5);
                p.lineTo(20, 12);
                p.lineTo(8, 19);
                p.closePath();
                g.fill(p);
            }
            case "pause" -> {
                g.fillRoundRect(7, 5, 4, 14, 1, 1);
                g.fillRoundRect(14, 5, 4, 14, 1, 1);
            }
            case "next" -> {
                Path2D p = new Path2D.Float();
                p.moveTo(5, 5);
                p.lineTo(16, 12);
                p.lineTo(5, 19);
                p.closePath();
                g.fill(p);
                g.drawLine(19, 5, 19, 19);
            }
            case "previous" -> {
                Path2D p = new Path2D.Float();
                p.moveTo(19, 5);
                p.lineTo(8, 12);
                p.lineTo(19, 19);
                p.closePath();
                g.fill(p);
                g.drawLine(5, 5, 5, 19);
            }
            case "home" -> {
                Path2D p = new Path2D.Float();
                p.moveTo(3, 10);
                p.lineTo(12, 3);
                p.lineTo(21, 10);
                p.moveTo(6, 9);
                p.lineTo(6, 21);
                p.lineTo(18, 21);
                p.lineTo(18, 9);
                p.moveTo(10, 21);
                p.lineTo(10, 14);
                p.lineTo(14, 14);
                p.lineTo(14, 21);
                g.draw(p);
            }
            case "heart" -> {
                Path2D p = new Path2D.Float();
                p.moveTo(12, 21);
                p.curveTo(-8, 8, 7, -3, 12, 7);
                p.curveTo(17, -3, 32, 8, 12, 21);
                g.draw(p);
            }
            case "search" -> {
                g.drawOval(3, 3, 13, 13);
                g.drawLine(16, 16, 22, 22);
            }
            case "plus" -> {
                g.drawLine(12, 4, 12, 20);
                g.drawLine(4, 12, 20, 12);
            }
            case "music" -> {
                g.drawLine(9, 17, 9, 5);
                g.drawLine(9, 5, 20, 3);
                g.drawLine(20, 3, 20, 15);
                g.drawOval(3, 16, 6, 5);
                g.drawOval(14, 14, 6, 5);
            }
            case "volume" -> {
                g.drawRect(3, 9, 4, 6);
                Path2D p = new Path2D.Float();
                p.moveTo(7, 9);
                p.lineTo(12, 5);
                p.lineTo(12, 19);
                p.lineTo(7, 15);
                g.draw(p);
                g.draw(new Arc2D.Float(10, 4, 12, 16, -65, 130, Arc2D.OPEN));
            }
            case "library" -> {
                g.drawRoundRect(3, 4, 5, 17, 2, 2);
                g.drawRoundRect(10, 4, 5, 17, 2, 2);
                g.drawLine(18, 4, 22, 20);
            }
            default -> {
                g.drawRoundRect(4, 4, 16, 16, 4, 4);
                g.drawLine(8, 9, 16, 9);
                g.drawLine(8, 15, 16, 15);
            }
        }
        g.dispose();
    }
}
