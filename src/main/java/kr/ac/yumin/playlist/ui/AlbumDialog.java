package kr.ac.yumin.playlist.ui;

import kr.ac.yumin.playlist.model.*;
import kr.ac.yumin.playlist.service.*;

import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

import javax.swing.*;

public final class AlbumDialog extends JDialog {
    private final MediaTrack track;
    private final FavoriteService favorites;
    private final Runnable changed;
    private final Consumer<MediaTrack> play;
    private final JPanel body = Ui.column();
    private final JButton star = Ui.quiet("☆ 즐겨찾기", this::toggleFavorite);
    private final JLabel status = Ui.muted("앨범 정보를 불러오고 있습니다.");
    private boolean favorite;

    public AlbumDialog(
            Window owner,
            MediaTrack track,
            Member member,
            Runnable changed,
            Consumer<MediaTrack> play) {
        super(owner, "앨범 정보 · " + track.song().album(), ModalityType.MODELESS);
        this.track = track;
        this.favorites = new FavoriteService(member);
        this.changed = changed;
        this.play = play;
        JPanel root = Ui.padded(26);
        JPanel header = new JPanel(new BorderLayout(24, 0));
        header.add(new JLabel(new AlbumArt(track, 170)), BorderLayout.WEST);
        JPanel words = Ui.column();
        words.add(Ui.muted("ALBUM"));
        words.add(Box.createVerticalStrut(10));
        words.add(wrapped(track.song().album(), 22, true));
        words.add(Box.createVerticalStrut(10));
        words.add(wrapped(track.song().artists(), 13, false));
        words.add(Box.createVerticalStrut(12));
        star.setToolTipText("이 곡을 즐겨찾기에 저장 / 해제");
        star.setEnabled(false);
        words.add(star);
        header.add(words, BorderLayout.CENTER);
        root.add(header, BorderLayout.NORTH);
        body.setBorder(BorderFactory.createEmptyBorder(24, 0, 0, 0));
        body.add(status);
        root.add(Ui.scroll(body), BorderLayout.CENTER);
        root.add(
                Ui.row(
                        Ui.primary("재생", () -> play.accept(track)),
                        Ui.quiet("정보 새로고침", () -> load(true)),
                        Ui.quiet("닫기", this::dispose)),
                BorderLayout.SOUTH);
        setContentPane(root);
        setSize(780, 740);
        setMinimumSize(new Dimension(680, 600));
        setLocationRelativeTo(owner);
        SwingUtilities.invokeLater(() -> load(false));
    }

    private record Data(AlbumDetail detail, boolean favorite) {}

    private void load(boolean refresh) {
        status.setText("앨범 정보를 불러오고 있습니다.");
        Ui.work(
                this,
                () ->
                        new Data(
                                new AlbumService().load(track.song().id(), refresh),
                                favorites.ids().contains(track.song().id())),
                data -> {
                    favorite = data.favorite();
                    updateStar();
                    star.setEnabled(true);
                    render(data.detail());
                    changed.run();
                });
    }

    private void render(AlbumDetail detail) {
        body.removeAll();
        body.add(Ui.text("앨범 정보", 16, true));
        body.add(Box.createVerticalStrut(12));
        fact(
                "발매일",
                detail.releaseDate() == null
                        ? null
                        : detail.releaseDate().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")));
        fact("앨범 유형", detail.type());
        fact("장르", detail.genre());
        fact("발매사", detail.distributor());
        fact("기획사", detail.agency());
        body.add(Box.createVerticalStrut(20));
        body.add(Ui.text("선택한 곡 · " + track.song().title(), 15, true));
        body.add(Box.createVerticalStrut(12));
        fact("작사", detail.people("작사"));
        fact("작곡", detail.people("작곡"));
        fact("편곡", detail.people("편곡"));
        body.add(Box.createVerticalStrut(18));
        body.add(Ui.text("이 보관함의 앨범 수록곡", 14, true));
        body.add(Box.createVerticalStrut(8));
        for (Song song : detail.songs()) body.add(wrapped("• " + song.title(), 12, false));
        body.add(Box.createVerticalStrut(18));
        body.add(
                Ui.muted(
                        "정보 출처: Melon · "
                                + detail.fetchedAt()
                                        .format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"))));
        body.revalidate();
        body.repaint();
    }

    private void fact(String label, String value) {
        JPanel line = new JPanel(new BorderLayout(15, 0));
        line.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel name = Ui.muted(label);
        name.setPreferredSize(new Dimension(78, 26));
        line.add(name, BorderLayout.WEST);
        line.add(
                wrapped(value == null || value.isBlank() ? "제공된 정보 없음" : value, 12, false),
                BorderLayout.CENTER);
        line.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, Math.max(32, line.getPreferredSize().height + 8)));
        body.add(line);
    }

    private static JLabel wrapped(String text, int size, boolean bold) {
        String safe = Ui.safe(text).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        JLabel label =
                Ui.text("<html><div style='width:390px'>" + safe + "</div></html>", size, bold);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private void toggleFavorite() {
        boolean next = !favorite;
        Ui.work(
                this,
                () -> {
                    favorites.set(track.song().id(), next);
                    return next;
                },
                saved -> {
                    favorite = saved;
                    updateStar();
                    changed.run();
                });
    }

    private void updateStar() {
        star.setText(favorite ? "★ 즐겨찾기 저장됨" : "☆ 즐겨찾기");
        star.setForeground(favorite ? new Color(177, 119, 6) : Ui.MUTED);
    }
}
