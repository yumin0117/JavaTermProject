package kr.ac.yumin.playlist.ui;

import kr.ac.yumin.playlist.model.Playlist;

import java.awt.*;
import java.util.function.Consumer;

import javax.swing.*;

public final class PlaylistEditor {
    public record Values(String title, String description, boolean isPublic) {}

    private PlaylistEditor() {}

    public static void open(Window owner, Playlist existing, Consumer<Values> submit) {
        JDialog d =
                new JDialog(
                        owner,
                        existing == null ? "플레이리스트 만들기" : "플레이리스트 수정",
                        Dialog.ModalityType.APPLICATION_MODAL);
        JPanel p = new JPanel(new BorderLayout(10, 18));
        p.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        JTextField title = new JTextField(existing == null ? "" : existing.title(), 30);
        JTextArea description =
                new JTextArea(existing == null ? "" : Ui.safe(existing.description()), 6, 30);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        JCheckBox pub = new JCheckBox("다른 회원에게 공개", existing != null && existing.isPublic());
        JPanel top = Ui.column();
        top.add(new JLabel("제목 (필수, 150자 이하)"));
        top.add(Box.createVerticalStrut(8));
        top.add(title);
        p.add(top, BorderLayout.NORTH);
        JPanel middle = new JPanel(new BorderLayout(0, 8));
        middle.add(new JLabel("설명 (1000자 이하)"), BorderLayout.NORTH);
        middle.add(new JScrollPane(description), BorderLayout.CENTER);
        p.add(middle, BorderLayout.CENTER);
        p.add(
                Ui.row(
                        pub,
                        Ui.button(
                                "저장",
                                () -> {
                                    Values v =
                                            new Values(
                                                    title.getText(),
                                                    description.getText(),
                                                    pub.isSelected());
                                    try {
                                        kr.ac.yumin.playlist.service.AuthService.text(
                                                v.title(), "제목", 150, true);
                                        kr.ac.yumin.playlist.service.AuthService.text(
                                                v.description(), "설명", 1000, false);
                                    } catch (RuntimeException e) {
                                        Ui.error(d, e);
                                        return;
                                    }
                                    d.dispose();
                                    submit.accept(v);
                                }),
                        Ui.button("취소", d::dispose)),
                BorderLayout.SOUTH);
        d.setContentPane(p);
        d.pack();
        d.setMinimumSize(new Dimension(540, 360));
        d.setLocationRelativeTo(owner);
        d.setVisible(true);
    }
}
