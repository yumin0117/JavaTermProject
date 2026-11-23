package kr.ac.yumin.playlist.ui;

import kr.ac.yumin.playlist.model.*;
import kr.ac.yumin.playlist.service.*;

import java.awt.*;
import java.awt.event.*;
import java.util.List;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class MainFrame extends JFrame {
    private final PlaylistService service;
    private List<Playlist> mine = List.of(), publicLists = List.of();
    private final DefaultTableModel ownModel = Ui.model("번호", "제목", "공개 여부", "좋아요", "생성일");
    private final DefaultTableModel publicModel = Ui.model("번호", "제목", "작성자", "좋아요", "내 좋아요");
    private final JTable ownTable = Ui.table(ownModel), publicTable = Ui.table(publicModel);
    private final JTextField query = new JTextField(25);
    private final JLabel status = new JLabel("목록을 불러오는 중입니다.");

    public MainFrame(Member member) {
        super("음악 플레이리스트 공유 시스템");
        service = new PlaylistService(member);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1120, 760);
        setMinimumSize(new Dimension(850, 580));
        JPanel root = new JPanel(new BorderLayout(0, 20));
        root.setBorder(BorderFactory.createEmptyBorder(25, 28, 20, 28));
        JPanel header = new JPanel(new BorderLayout());
        header.add(Ui.heading("나의 음악, 우리의 플레이리스트"), BorderLayout.WEST);
        header.add(
                Ui.row(
                        new JLabel(member.nickname() + " 님"),
                        Ui.button(
                                "로그아웃",
                                () -> {
                                    dispose();
                                    new AuthFrame().setVisible(true);
                                })),
                BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        JPanel own = new JPanel(new BorderLayout(0, 10));
        own.add(
                Ui.row(
                        Ui.button(
                                "새 플레이리스트",
                                () ->
                                        PlaylistEditor.open(
                                                this,
                                                null,
                                                v ->
                                                        Ui.work(
                                                                this,
                                                                () ->
                                                                        service.create(
                                                                                v.title(),
                                                                                v.description(),
                                                                                v.isPublic()),
                                                                id -> refresh()))),
                        Ui.button("상세 보기 / 곡 관리", () -> openSelected(true)),
                        Ui.button("수정", this::edit),
                        Ui.button("삭제", this::delete),
                        Ui.button("새로고침", this::refresh)),
                BorderLayout.NORTH);
        own.add(new JScrollPane(ownTable), BorderLayout.CENTER);
        tabs.addTab("내 플레이리스트", own);
        JPanel shared = new JPanel(new BorderLayout(0, 10));
        shared.add(
                Ui.row(
                        new JLabel("제목 검색"),
                        query,
                        Ui.button("검색", this::refresh),
                        Ui.button("상세 보기", () -> openSelected(false)),
                        Ui.button("좋아요 / 취소", this::like)),
                BorderLayout.NORTH);
        shared.add(new JScrollPane(publicTable), BorderLayout.CENTER);
        tabs.addTab("공개 플레이리스트", shared);
        root.add(tabs, BorderLayout.CENTER);
        root.add(status, BorderLayout.SOUTH);
        setContentPane(root);
        setLocationRelativeTo(null);
        query.addActionListener(e -> refresh());
        ownTable.addMouseListener(
                new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        if (e.getClickCount() == 2) openSelected(true);
                    }
                });
        publicTable.addMouseListener(
                new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        if (e.getClickCount() == 2) openSelected(false);
                    }
                });
        SwingUtilities.invokeLater(this::refresh);
    }

    private record Lists(List<Playlist> mine, List<Playlist> shared) {}

    public void refresh() {
        String q = query.getText();
        status.setText("목록을 불러오는 중입니다.");
        Ui.work(
                this,
                () -> new Lists(service.mine(), service.discover(q)),
                data -> {
                    mine = data.mine();
                    publicLists = data.shared();
                    ownModel.setRowCount(0);
                    publicModel.setRowCount(0);
                    for (Playlist p : mine)
                        ownModel.addRow(
                                new Object[] {
                                    p.id(),
                                    p.title(),
                                    p.isPublic() ? "공개" : "비공개",
                                    p.likes(),
                                    p.createdAt().toLocalDate()
                                });
                    for (Playlist p : publicLists)
                        publicModel.addRow(
                                new Object[] {
                                    p.id(),
                                    p.title(),
                                    p.nickname(),
                                    p.likes(),
                                    p.liked() ? "좋아요 누름" : "-"
                                });
                    status.setText(
                            "내 목록 "
                                    + mine.size()
                                    + "개  |  공개 검색 결과 "
                                    + publicLists.size()
                                    + "개  |  목록을 선택하여 상세 화면에서 곡을 관리하세요.");
                });
    }

    private Playlist selected(boolean own) {
        JTable table = own ? ownTable : publicTable;
        int row = table.getSelectedRow();
        if (row < 0) throw new AppException("플레이리스트를 먼저 선택해 주세요.");
        return (own ? mine : publicLists).get(table.convertRowIndexToModel(row));
    }

    private void openSelected(boolean own) {
        try {
            new DetailDialog(this, service, selected(own).id(), this::refresh).setVisible(true);
        } catch (AppException e) {
            Ui.error(this, e);
        }
    }

    private void edit() {
        try {
            Playlist p = selected(true);
            PlaylistEditor.open(
                    this,
                    p,
                    v ->
                            Ui.work(
                                    this,
                                    () -> {
                                        service.edit(
                                                p.id(), v.title(), v.description(), v.isPublic());
                                        return true;
                                    },
                                    ok -> refresh()));
        } catch (AppException e) {
            Ui.error(this, e);
        }
    }

    private void delete() {
        try {
            Playlist p = selected(true);
            if (JOptionPane.showConfirmDialog(
                            this,
                            "‘" + p.title() + "’ 목록과 수록곡 연결, 좋아요 내역을 삭제할까요?",
                            "삭제 확인",
                            JOptionPane.YES_NO_OPTION)
                    != JOptionPane.YES_OPTION) return;
            Ui.work(
                    this,
                    () -> {
                        service.delete(p.id());
                        return true;
                    },
                    ok -> refresh());
        } catch (AppException e) {
            Ui.error(this, e);
        }
    }

    private void like() {
        try {
            Playlist p = selected(false);
            Ui.work(
                    this,
                    () -> {
                        service.like(p.id(), !p.liked());
                        return true;
                    },
                    ok -> refresh());
        } catch (AppException e) {
            Ui.error(this, e);
        }
    }
}
