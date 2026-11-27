package kr.ac.yumin.playlist.ui;

import kr.ac.yumin.playlist.model.*;
import kr.ac.yumin.playlist.service.*;

import java.awt.*;
import java.util.List;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class DetailDialog extends JDialog {
    private final PlaylistService service;
    private final java.util.function.Consumer<Song> listen;
    private final long id;
    private final Runnable changed;
    private PlaylistDetail detail;
    private List<Song> searchResults = List.of();
    private final JLabel title = Ui.heading("플레이리스트 불러오는 중"), meta = new JLabel(" ");
    private final JTextArea description = new JTextArea(2, 30);
    private final DefaultTableModel model = Ui.model("순서", "곡 제목", "아티스트", "앨범", "장르", "재생시간");
    private final DefaultTableModel searchModel = Ui.model("곡 제목", "아티스트", "앨범", "장르", "재생시간");
    private final JTable table = Ui.table(model), searchTable = Ui.table(searchModel);
    private final JPanel manage = new JPanel(new BorderLayout(0, 8));
    private final JButton like = Ui.button("좋아요", this::like);
    private final JTextField query = new JTextField(25);

    public DetailDialog(Window owner, PlaylistService service, long id, Runnable changed) {
        this(owner, service, id, changed, song -> {});
    }

    public DetailDialog(
            Window owner,
            PlaylistService service,
            long id,
            Runnable changed,
            java.util.function.Consumer<Song> listen) {
        super(owner, "플레이리스트 상세", ModalityType.APPLICATION_MODAL);
        this.listen = listen;
        this.service = service;
        this.id = id;
        this.changed = changed;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1100, 790);
        setMinimumSize(new Dimension(850, 620));
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        JPanel heading = Ui.column();
        heading.add(title);
        heading.add(Box.createVerticalStrut(10));
        heading.add(meta);
        heading.add(Box.createVerticalStrut(10));
        description.setEditable(false);
        description.setFocusable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        heading.add(new JScrollPane(description));
        JPanel header = new JPanel(new BorderLayout(22, 0));
        header.add(new MusicArt(id, 115), BorderLayout.WEST);
        header.add(heading, BorderLayout.CENTER);
        root.add(header, BorderLayout.NORTH);
        JPanel contents = new JPanel(new BorderLayout(0, 8));
        contents.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel ownButtons =
                Ui.row(
                        Ui.button("선택 곡 제거", () -> modify(0)),
                        Ui.button("위로 이동", () -> modify(-1)),
                        Ui.button("아래로 이동", () -> modify(1)));
        manage.add(ownButtons, BorderLayout.NORTH);
        JPanel search = new JPanel(new BorderLayout(0, 8));
        search.add(
                Ui.row(
                        new JLabel("곡 제목 / 아티스트"),
                        query,
                        Ui.button("곡 검색", this::search),
                        Ui.button("선택 곡 추가", this::add)),
                BorderLayout.NORTH);
        JScrollPane results = new JScrollPane(searchTable);
        results.setPreferredSize(new Dimension(700, 175));
        search.add(results, BorderLayout.CENTER);
        manage.add(search, BorderLayout.CENTER);
        contents.add(manage, BorderLayout.SOUTH);
        root.add(contents, BorderLayout.CENTER);
        root.add(
                Ui.row(
                        Ui.primary(
                                "선택 곡 듣기",
                                () -> {
                                    int row = table.getSelectedRow();
                                    if (row >= 0 && detail != null) {
                                        dispose();
                                        listen.accept(detail.songs().get(row));
                                    }
                                }),
                        like,
                        Ui.button("새로고침", this::reload),
                        Ui.button("닫기", this::dispose)),
                BorderLayout.SOUTH);
        manage.setVisible(false);
        like.setEnabled(false);
        setContentPane(root);
        setLocationRelativeTo(owner);
        query.addActionListener(e -> search());
        SwingUtilities.invokeLater(this::reload);
    }

    public void reload() {
        Ui.work(
                this,
                () -> service.detail(id),
                data -> {
                    detail = data;
                    Playlist p = data.playlist();
                    title.setText(p.title());
                    meta.setText(
                            "작성자: "
                                    + p.nickname()
                                    + "    |    "
                                    + (p.isPublic() ? "공개" : "비공개")
                                    + "    |    좋아요 "
                                    + p.likes()
                                    + "개");
                    description.setText(Ui.safe(p.description()));
                    model.setRowCount(0);
                    for (Song s : data.songs())
                        model.addRow(
                                new Object[] {
                                    s.order(),
                                    s.title(),
                                    s.artists(),
                                    s.album(),
                                    s.genre(),
                                    s.duration()
                                });
                    boolean own = p.owner().equals(service.member().id());
                    manage.setVisible(own);
                    like.setEnabled(p.isPublic() || p.liked());
                    like.setText(p.liked() ? "좋아요 취소" : "좋아요");
                    revalidate();
                    if (own && searchResults.isEmpty()) search();
                });
    }

    private void search() {
        String q = query.getText();
        Ui.work(
                this,
                () -> service.searchPlayable(q),
                data -> {
                    searchResults = data;
                    searchModel.setRowCount(0);
                    for (Song s : data)
                        searchModel.addRow(
                                new Object[] {
                                    s.title(), s.artists(), s.album(), s.genre(), s.duration()
                                });
                });
    }

    private void add() {
        int row = searchTable.getSelectedRow();
        if (row < 0) {
            Ui.info(this, "추가할 검색 결과 곡을 선택해 주세요.");
            return;
        }
        long song = searchResults.get(searchTable.convertRowIndexToModel(row)).id();
        Ui.work(
                this,
                () -> {
                    service.add(id, song);
                    return true;
                },
                ok -> {
                    reload();
                    changed.run();
                });
    }

    private void modify(int direction) {
        int row = table.getSelectedRow();
        if (row < 0 || detail == null) {
            Ui.info(this, "수록 곡을 선택해 주세요.");
            return;
        }
        long song = detail.songs().get(table.convertRowIndexToModel(row)).id();
        Ui.work(
                this,
                () -> {
                    if (direction == 0) service.remove(id, song);
                    else service.move(id, song, direction);
                    return true;
                },
                ok -> {
                    reload();
                    changed.run();
                });
    }

    private void like() {
        if (detail == null) return;
        boolean value = !detail.playlist().liked();
        Ui.work(
                this,
                () -> {
                    service.like(id, value);
                    return true;
                },
                ok -> {
                    reload();
                    changed.run();
                });
    }
}
