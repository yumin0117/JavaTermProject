package kr.ac.yumin.playlist.ui;

import kr.ac.yumin.playlist.audio.AudioPlayer;
import kr.ac.yumin.playlist.model.*;
import kr.ac.yumin.playlist.service.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.table.*;

/** 로그인 화면 없이 열리는 음악 앱. 목록·이용권·저장은 모두 Oracle 서비스에서 읽는다. */
public final class MainFrame extends JFrame {
    private Member member;
    private PlaylistService playlists;
    private MusicLibraryService library;
    private MembershipService membership;
    private List<MediaTrack> tracks = List.of(), shownTracks = List.of();
    private List<Playlist> mine = List.of(), shared = List.of();
    private PassStatus pass = PassStatus.free();
    private final AudioPlayer player = new AudioPlayer();
    private final JPanel content = new JPanel(new BorderLayout()), sidebar = Ui.column();
    private final JLabel connection = Ui.muted("보관함을 연결하는 중입니다."),
            sectionTitle = Ui.text("둘러보기", 15, true);
    private final JTextField search = new JTextField(23);
    private final Map<String, JButton> navigation = new LinkedHashMap<>();
    private final JLabel nowTitle = Ui.text("음악을 선택해 주세요", 13, true),
            nowArtist = Ui.muted("좋아하는 곡을 찾아 들어보세요"),
            time = Ui.muted("0:00 / 0:00"),
            playMode = Ui.muted("무료 · 1분 미리듣기");
    private final JLabel nowArt = new JLabel(new MusicArt(0, 48));
    private final JButton playButton, planButton;
    private final JSlider progress = new JSlider(0, 60, 0);
    private final javax.swing.Timer clock;
    private String page = "홈", filter = "전체";
    private boolean loading, updatingSlider, limitAnnounced;
    private MediaTrack selected;
    private OnlineVideoPanel onlinePlayer;
    private boolean onlineMode;
    private Set<Long> favoriteIds = Set.of();
    private Map<Long, String> videoStates = Map.of();
    private final JButton favoriteButton = Ui.quiet("☆", () -> toggleFavorite(footerTrack()));

    public MainFrame() {
        this(null);
    }

    public MainFrame(Member initialMember) {
        super("muse · 음악 플레이리스트");
        member = initialMember;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1280, 900);
        setMinimumSize(new Dimension(1060, 740));
        JPanel root = new JPanel(new BorderLayout());
        root.add(buildSidebar(), BorderLayout.WEST);
        JPanel main = new JPanel(new BorderLayout());
        main.add(buildHeader(), BorderLayout.NORTH);
        main.add(content, BorderLayout.CENTER);
        root.add(main, BorderLayout.CENTER);
        playButton =
                iconButton(
                        "play",
                        24,
                        Ui.ACCENT,
                        () -> {
                            if (onlineMode
                                    && onlinePlayer != null
                                    && selected != null
                                    && onlinePlayer.link() != null
                                    && onlinePlayer.link().songId() == selected.song().id()) {
                                if (!page.equals("지금 재생 중")) navigate("지금 재생 중");
                                onlinePlayer.togglePause();
                            } else if (player.playing()) {
                                player.togglePause();
                            } else if (selected != null) play(selected);
                        });
        planButton = Ui.primary("Plus 이용권", this::openMembership);
        root.add(buildPlayer(), BorderLayout.SOUTH);
        setContentPane(root);
        setLocationRelativeTo(null);
        search.putClientProperty("JTextField.placeholderText", "곡, 가수, 앨범 검색");
        search.putClientProperty("JTextField.leadingIcon", new MuseIcon("search", 16, Ui.MUTED));
        search.addActionListener(
                e -> {
                    page = "전체 곡";
                    render();
                });
        clock = new javax.swing.Timer(200, e -> tick());
        clock.start();
        addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        clock.stop();
                        player.close();
                        if (onlinePlayer != null) onlinePlayer.close();
                    }
                });
        render();
        SwingUtilities.invokeLater(this::refresh);
    }

    private JPanel buildSidebar() {
        sidebar.setBackground(new Color(247, 247, 249));
        sidebar.setPreferredSize(new Dimension(226, 650));
        sidebar.setBorder(BorderFactory.createEmptyBorder(30, 18, 22, 18));
        JLabel logo = Ui.text("muse.", 36, true);
        logo.setForeground(Ui.ACCENT);
        logo.setBorder(BorderFactory.createEmptyBorder(0, 12, 28, 0));
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(logo);
        JLabel tag = Ui.muted("나의 일상에, 음악");
        tag.setBorder(BorderFactory.createEmptyBorder(0, 13, 20, 0));
        sidebar.add(tag);
        String[][] items = {
            {"홈", "home"},
            {"전체 곡", "music"},
            {"지금 재생 중", "play"},
            {"즐겨찾기 곡", "star"},
            {"내 플레이리스트", "library"},
            {"공개 플레이리스트", "search"}
        };
        for (String[] item : items) {
            JButton b = Ui.button(item[0], () -> navigate(item[0]));
            b.setIcon(new MuseIcon(item[1], 19, Ui.MUTED));
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setIconTextGap(12);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            navigation.put(item[0], b);
            sidebar.add(b);
            sidebar.add(Box.createVerticalStrut(5));
        }
        sidebar.add(Box.createVerticalStrut(24));
        JButton create = Ui.quiet("새 플레이리스트", this::create);
        create.setIcon(new MuseIcon("plus", 17, Ui.ACCENT));
        create.setBackground(sidebar.getBackground());
        create.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        sidebar.add(create);
        sidebar.add(Box.createVerticalGlue());
        JPanel plus = new JPanel(new BorderLayout(0, 8));
        plus.setBackground(new Color(237, 235, 242));
        plus.setBorder(BorderFactory.createEmptyBorder(16, 14, 14, 14));
        plus.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        JPanel words = Ui.column();
        words.setOpaque(false);
        words.add(Ui.text("공식 뮤직비디오", 18, true));
        words.add(Box.createVerticalStrut(8));
        words.add(Ui.muted("앱 안에서 음악과 영상을 함께."));
        plus.add(words, BorderLayout.CENTER);
        JButton more = Ui.button("지금 재생 중 보기", () -> navigate("지금 재생 중"));
        more.setFont(more.getFont().deriveFont(11f));
        plus.add(more, BorderLayout.SOUTH);
        sidebar.add(plus);
        sidebar.add(Box.createVerticalStrut(24));
        sidebar.add(Ui.text("나의 보관함", 13, true));
        sidebar.add(Box.createVerticalStrut(5));
        sidebar.add(Ui.muted("이 PC의 개인 음악 공간"));
        for (Component child : sidebar.getComponents())
            if (child instanceof JComponent component)
                component.setAlignmentX(Component.LEFT_ALIGNMENT);
        return sidebar;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(15, 0));
        header.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(239, 239, 243)),
                        BorderFactory.createEmptyBorder(17, 30, 16, 30)));
        header.add(sectionTitle, BorderLayout.WEST);
        JPanel tools = Ui.row(search);
        tools.setBorder(BorderFactory.createEmptyBorder());
        header.add(tools, BorderLayout.EAST);
        return header;
    }

    private JPanel buildPlayer() {
        JPanel footer = new JPanel(new BorderLayout(24, 0));
        footer.setPreferredSize(new Dimension(1100, 98));
        footer.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(234, 234, 240)),
                        BorderFactory.createEmptyBorder(15, 24, 14, 24)));
        JPanel left = new JPanel(new BorderLayout(12, 0));
        left.setPreferredSize(new Dimension(300, 58));
        nowArt.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        nowArt.setToolTipText("앨범 정보 보기");
        nowArt.addMouseListener(
                new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        openAlbum(footerTrack());
                    }
                });
        favoriteButton.setFont(favoriteButton.getFont().deriveFont(25f));
        favoriteButton.setToolTipText("즐겨찾기 저장 / 해제");
        favoriteButton.setEnabled(false);
        left.add(favoriteButton, BorderLayout.EAST);
        left.add(nowArt, BorderLayout.WEST);
        JPanel names = Ui.column();
        names.add(Box.createVerticalStrut(7));
        names.add(nowTitle);
        names.add(Box.createVerticalStrut(5));
        names.add(nowArtist);
        left.add(names, BorderLayout.CENTER);
        footer.add(left, BorderLayout.WEST);
        JPanel middle = new JPanel(new BorderLayout(8, 0));
        middle.add(
                Ui.row(
                        iconButton("previous", 18, Ui.MUTED, () -> skip(-1)),
                        playButton,
                        iconButton("next", 18, Ui.MUTED, () -> skip(1))),
                BorderLayout.WEST);
        JPanel seek = Ui.column();
        JPanel labels = new JPanel(new BorderLayout());
        labels.add(playMode, BorderLayout.WEST);
        labels.add(time, BorderLayout.EAST);
        seek.add(Box.createVerticalStrut(8));
        seek.add(labels);
        seek.add(progress);
        middle.add(seek, BorderLayout.CENTER);
        footer.add(middle, BorderLayout.CENTER);
        JSlider volume = new JSlider(0, 100, 65);
        volume.setPreferredSize(new Dimension(85, 25));
        volume.setToolTipText("음량");
        volume.addChangeListener(
                e -> {
                    player.volume(volume.getValue() / 100f);
                    if (onlinePlayer != null) onlinePlayer.volume(volume.getValue() / 100f);
                });
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        right.add(new JLabel(new MuseIcon("volume", 19, Ui.MUTED)));
        right.add(Box.createHorizontalStrut(8));
        right.add(volume);
        right.add(Box.createHorizontalStrut(10));
        right.add(planButton);
        right.setPreferredSize(new Dimension(280, 55));
        footer.add(right, BorderLayout.EAST);
        progress.setToolTipText("재생 위치 · 무료 이용은 1분까지 이동할 수 있습니다.");
        progress.addChangeListener(
                e -> {
                    if (!updatingSlider && !progress.getValueIsAdjusting()) {
                        if (onlineMode && onlinePlayer != null)
                            onlinePlayer.seek(progress.getValue());
                        else player.seek(progress.getValue());
                    }
                });
        return footer;
    }

    private JButton iconButton(String name, int size, Color color, Runnable action) {
        JButton b = Ui.quiet("", action);
        b.setIcon(new MuseIcon(name, size, color));
        b.setMargin(new Insets(8, 7, 8, 7));
        b.setToolTipText(
                switch (name) {
                    case "play" -> "재생 / 일시정지";
                    case "next" -> "다음 곡";
                    case "previous" -> "이전 곡";
                    default -> name;
                });
        return b;
    }

    private void navigate(String next) {
        if (onlinePlayer != null && page.equals("지금 재생 중") && !next.equals("지금 재생 중"))
            onlinePlayer.pause();
        page = next;
        filter = "전체";
        search.setText("");
        render();
    }

    public void refresh() {
        if (loading) return;
        loading = true;
        connection.setText("음악 보관함을 불러오고 있습니다.");
        Member current = member;
        new SwingWorker<Loaded, Void>() {
            protected Loaded doInBackground() throws Exception {
                Member resolved = current == null ? new LocalProfileService().resolve() : current;
                PlaylistService ps = new PlaylistService(resolved);
                MusicLibraryService ms = new MusicLibraryService(resolved);
                MembershipService membershipService = new MembershipService(resolved);
                List<Playlist> publicLists =
                        ps.discover("").stream()
                                .filter(
                                        p ->
                                                p.owner().startsWith("local_")
                                                        || p.owner().equals(resolved.id()))
                                .toList();
                return new Loaded(
                        resolved,
                        ps,
                        ms,
                        membershipService,
                        ms.catalog(),
                        ps.mine(),
                        publicLists,
                        membershipService.status(),
                        new FavoriteService(resolved).ids(),
                        new OnlineVideoService().states());
            }

            protected void done() {
                loading = false;
                if (!isDisplayable()) return;
                try {
                    Loaded d = get();
                    member = d.member();
                    playlists = d.playlists();
                    library = d.library();
                    membership = d.membership();
                    tracks = d.tracks();
                    mine = d.mine();
                    shared = d.shared();
                    pass = d.pass();
                    favoriteIds = d.favorites();
                    videoStates = d.videos();
                    player.updatePass(pass);
                    if (onlinePlayer != null) onlinePlayer.updatePass(pass);
                    connection.setText("");
                } catch (Exception e) {
                    library = null;
                    playlists = null;
                    membership = null;
                    tracks = List.of();
                    mine = List.of();
                    shared = List.of();
                    pass = PassStatus.free();
                    player.stop();
                    connection.setText("음악 보관함을 불러오지 못했습니다. 잠시 후 다시 실행해 주세요.");
                }
                render();
            }
        }.execute();
    }

    private record Loaded(
            Member member,
            PlaylistService playlists,
            MusicLibraryService library,
            MembershipService membership,
            List<MediaTrack> tracks,
            List<Playlist> mine,
            List<Playlist> shared,
            PassStatus pass,
            Set<Long> favorites,
            Map<Long, String> videos) {}

    private void render() {
        if (onlinePlayer != null && !page.equals("지금 재생 중")) onlinePlayer.pause();
        for (var entry : navigation.entrySet()) {
            boolean on = entry.getKey().equals(page);
            entry.getValue()
                    .putClientProperty(
                            "FlatLaf.style",
                            "background:"
                                    + (on ? "#FCE7EC" : "#F7F7F9")
                                    + "; foreground:"
                                    + (on ? "#D73250" : "#757681")
                                    + "; borderWidth:0; arc:12");
        }
        sectionTitle.setText(page);
        content.removeAll();
        if (page.equals("지금 재생 중")) {
            if (onlinePlayer == null) {
                content.add(
                        empty("재생할 곡을 선택해 주세요", "곡을 더블클릭하거나 하단 재생 버튼을 누르면 공식 영상이 앱 안에서 열립니다."),
                        BorderLayout.CENTER);
            } else content.add(onlinePlayer, BorderLayout.CENTER);
            content.revalidate();
            content.repaint();
            updatePassLabel();
            updateFavoriteButton();
            return;
        }
        JPanel body = Ui.column();
        body.setBorder(BorderFactory.createEmptyBorder(24, 32, 26, 32));
        if (!connection.getText().isBlank()) {
            JPanel banner = new JPanel(new BorderLayout());
            banner.setAlignmentX(Component.LEFT_ALIGNMENT);
            banner.add(connection, BorderLayout.CENTER);
            banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            body.add(banner);
            body.add(Box.createVerticalStrut(17));
        }
        if (page.equals("홈")) renderHome(body);
        else if (page.equals("전체 곡") || page.equals("즐겨찾기 곡")) renderSongs(body);
        else renderPlaylists(body);
        content.add(Ui.scroll(body), BorderLayout.CENTER);
        content.revalidate();
        content.repaint();
        updatePassLabel();
        updateFavoriteButton();
    }

    private void append(JPanel body, JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(component);
    }

    private void renderHome(JPanel body) {
        append(body, Ui.text("오늘, 어떤 음악을 들을까요?", 27, true));
        body.add(Box.createVerticalStrut(8));
        append(body, Ui.muted("익숙한 명곡부터 새로운 취향까지, 음악이 머무는 곳."));
        body.add(Box.createVerticalStrut(23));
        JPanel hero =
                new JPanel(new BorderLayout()) {
                    protected void paintComponent(Graphics graphics) {
                        Graphics2D g = (Graphics2D) graphics.create();
                        g.setRenderingHint(
                                RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g.setPaint(
                                new GradientPaint(
                                        0,
                                        0,
                                        new Color(31, 54, 58),
                                        getWidth(),
                                        getHeight(),
                                        new Color(75, 106, 96)));
                        g.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                        g.setColor(new Color(223, 223, 178, 40));
                        for (int i = 0; i < 9; i++)
                            g.drawOval(
                                    getWidth() - 260 - i * 18,
                                    -50 - i * 18,
                                    280 + i * 36,
                                    280 + i * 36);
                        g.dispose();
                    }
                };
        hero.setOpaque(false);
        hero.setBorder(BorderFactory.createEmptyBorder(21, 26, 20, 26));
        hero.setPreferredSize(new Dimension(800, 166));
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 166));
        JPanel heroText = Ui.column();
        heroText.setOpaque(false);
        JLabel eyebrow = Ui.text("MUSE · YOUR MUSIC LIBRARY", 10, true);
        eyebrow.setForeground(new Color(205, 221, 209));
        heroText.add(eyebrow);
        heroText.add(Box.createVerticalStrut(9));
        JLabel title = Ui.text("좋아하는 음악을 한곳에", 26, true);
        title.setForeground(Color.WHITE);
        heroText.add(title);
        heroText.add(Box.createVerticalStrut(10));
        JLabel subtitle = Ui.text("나만의 취향을 발견하세요.", 12, false);
        subtitle.setForeground(new Color(221, 229, 222));
        heroText.add(subtitle);
        heroText.add(Box.createVerticalStrut(12));
        JButton listen = Ui.button("음악 둘러보기  →", () -> navigate("전체 곡"));
        listen.putClientProperty(
                "FlatLaf.style",
                "background:#FFFFFF;foreground:#29473D;arc:999;borderWidth:0;margin:5,14,5,14");
        heroText.add(listen);
        hero.add(heroText, BorderLayout.CENTER);
        JLabel disc =
                new JLabel(
                        tracks.isEmpty() ? new MusicArt(0, 120) : new AlbumArt(tracks.get(0), 120));
        disc.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 10));
        hero.add(disc, BorderLayout.EAST);
        disc.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        disc.setToolTipText("앨범 정보 보기");
        disc.addMouseListener(
                new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        if (!tracks.isEmpty()) openAlbum(tracks.get(0));
                    }
                });
        append(body, hero);
        body.add(Box.createVerticalStrut(24));
        append(body, section("지금 만나볼 음악", Ui.quiet("전체 보기  →", () -> navigate("전체 곡"))));
        body.add(Box.createVerticalStrut(13));
        if (tracks.isEmpty()) append(body, empty("음악 보관함을 연결해 주세요", "연결하면 실제 곡과 앨범이 이곳에 표시됩니다."));
        else {
            JPanel cards = new JPanel(new GridLayout(1, 4, 18, 0));
            cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 255));
            cards.setPreferredSize(new Dimension(800, 255));
            for (MediaTrack t : tracks.stream().limit(4).toList()) cards.add(trackCard(t));
            append(body, cards);
        }
        body.add(Box.createVerticalStrut(23));
        append(body, section("나의 음악", Ui.quiet("선택한 곡 담기", () -> addSelected())));
        body.add(Box.createVerticalStrut(9));
        append(body, songTable(tracks));
    }

    private JPanel section(String title, JButton action) {
        JPanel p = new JPanel(new BorderLayout());
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        p.add(Ui.text(title, 19, true), BorderLayout.WEST);
        p.add(action, BorderLayout.EAST);
        return p;
    }

    private JPanel trackCard(MediaTrack track) {
        JPanel card = new JPanel(new BorderLayout(0, 9));
        JLabel cover = new JLabel(new AlbumArt(track, 175));
        card.add(cover, BorderLayout.CENTER);
        JPanel words = Ui.column();
        JLabel title = Ui.text(track.song().title(), 12, true);
        title.setToolTipText(track.song().title());
        words.add(title);
        words.add(Box.createVerticalStrut(5));
        words.add(Ui.muted(track.song().artists()));
        words.add(
                Ui.quiet(
                        track.playable() ? "▶ 재생" : videoLabel(track.song().id()),
                        () -> play(track)));
        card.add(words, BorderLayout.SOUTH);
        MouseAdapter click =
                new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        select(track);
                        openAlbum(track);
                    }
                };
        cover.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cover.addMouseListener(click);
        card.setToolTipText("앨범 정보 보기 · " + track.song().title());
        return card;
    }

    private void renderSongs(JPanel body) {
        append(body, Ui.text(page.equals("즐겨찾기 곡") ? "즐겨찾기 곡" : "음악 보관함", 29, true));
        body.add(Box.createVerticalStrut(9));
        append(body, Ui.muted("앨범 커버로 자세히 살펴보고, 별표로 좋아하는 곡을 모아보세요."));
        body.add(Box.createVerticalStrut(22));
        JPanel pills = Ui.row();
        pills.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        for (String category : new String[] {"전체", "즐겨찾기"}) {
            JButton b =
                    Ui.button(
                            category,
                            () -> {
                                filter = category;
                                render();
                            });
            b.putClientProperty("FlatLaf.styleClass", category.equals(filter) ? "primary" : "pill");
            pills.add(b);
        }
        append(body, pills);
        body.add(Box.createVerticalStrut(14));
        String q = search.getText().strip().toLowerCase(Locale.ROOT);
        shownTracks =
                tracks.stream()
                        .filter(t -> !page.equals("즐겨찾기 곡") || favoriteIds.contains(t.song().id()))
                        .filter(
                                t ->
                                        switch (filter) {
                                            case "즐겨찾기" -> favoriteIds.contains(t.song().id());
                                            case "MP3 연결됨" -> t.playable();
                                            default -> true;
                                        })
                        .filter(
                                t ->
                                        (t.song().title()
                                                        + " "
                                                        + t.song().artists()
                                                        + " "
                                                        + t.song().album())
                                                .toLowerCase(Locale.ROOT)
                                                .contains(q))
                        .toList();
        append(
                body,
                section("모든 곡  ·  " + shownTracks.size(), Ui.quiet("선택한 곡 담기", this::addSelected)));
        body.add(Box.createVerticalStrut(10));
        append(body, songTable(shownTracks));
        if (shownTracks.isEmpty())
            append(
                    body,
                    empty(
                            tracks.isEmpty() ? "연결된 음악이 없습니다" : "검색 결과가 없습니다",
                            tracks.isEmpty()
                                    ? "음악 보관함을 불러온 뒤 다시 확인해 주세요."
                                    : "다른 제목이나 가수 이름으로 검색해 보세요."));
    }

    private String videoLabel(long id) {
        return switch (videoStates.getOrDefault(id, "SEARCH")) {
            case "PLAYABLE" -> "▶ 영상";
            case "METADATA" -> "영상 확인";
            case "UNAVAILABLE" -> "재생 제한";
            default -> "미연결";
        };
    }

    private JComponent songTable(List<MediaTrack> rows) {
        DefaultTableModel model = Ui.model("#", "곡 / 가수", "앨범", "재생", "담기", "별");
        for (int i = 0; i < rows.size(); i++) {
            Song s = rows.get(i).song();
            model.addRow(
                    new Object[] {
                        i + 1,
                        s.title(),
                        s.album(),
                        rows.get(i).playable() ? s.duration() : videoLabel(s.id()),
                        "+",
                        favoriteIds.contains(s.id()) ? "★" : "☆"
                    });
        }
        JTable table = Ui.table(model);
        table.setRowHeight(58);
        table.getColumnModel().getColumn(0).setMaxWidth(42);
        table.getColumnModel().getColumn(3).setMaxWidth(90);
        table.getColumnModel().getColumn(4).setMaxWidth(42);
        table.getColumnModel().getColumn(5).setMaxWidth(42);
        table.getColumnModel()
                .getColumn(5)
                .setCellRenderer(
                        (t, value, on, focus, row, column) -> {
                            JLabel star = new JLabel(String.valueOf(value), SwingConstants.CENTER);
                            star.setFont(star.getFont().deriveFont(21f));
                            star.setForeground(
                                    favoriteIds.contains(rows.get(row).song().id())
                                            ? new Color(177, 119, 6)
                                            : Ui.MUTED);
                            return star;
                        });
        table.getColumnModel().getColumn(1).setPreferredWidth(340);
        table.getColumnModel().getColumn(2).setPreferredWidth(240);
        table.getColumnModel()
                .getColumn(1)
                .setCellRenderer(
                        (t, value, isSelected, focus, row, column) -> {
                            MediaTrack track = rows.get(row);
                            JPanel panel = new JPanel(new BorderLayout(11, 0));
                            panel.setBackground(
                                    isSelected ? new Color(255, 240, 243) : Color.WHITE);
                            panel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
                            panel.add(new JLabel(new AlbumArt(track, 40)), BorderLayout.WEST);
                            JPanel text = Ui.column();
                            text.setOpaque(false);
                            text.add(Box.createVerticalStrut(3));
                            text.add(Ui.text(track.song().title(), 12, true));
                            text.add(Box.createVerticalStrut(4));
                            text.add(Ui.muted(track.song().artists()));
                            panel.add(text, BorderLayout.CENTER);
                            return panel;
                        });
        table.getSelectionModel()
                .addListSelectionListener(
                        e -> {
                            int row = table.getSelectedRow();
                            if (row >= 0) select(rows.get(row));
                        });
        table.addMouseListener(
                new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        int row = table.rowAtPoint(e.getPoint());
                        if (row < 0) return;
                        select(rows.get(row));
                        int column = table.columnAtPoint(e.getPoint());
                        if (column == 5) {
                            if (e.getClickCount() == 1) toggleFavorite(rows.get(row));
                        } else if (column == 4) {
                            if (e.getClickCount() == 1) addSelected();
                        } else if (column == 3) {
                            if (e.getClickCount() == 1) play(rows.get(row));
                        } else if (column == 2
                                || (column == 1
                                        && e.getX() - table.getCellRect(row, 1, true).x < 45)) {
                            if (e.getClickCount() == 1) openAlbum(rows.get(row));
                        } else if (e.getClickCount() == 2) play(rows.get(row));
                    }
                });
        table.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "listen");
        table.getActionMap()
                .put(
                        "listen",
                        new AbstractAction() {
                            public void actionPerformed(ActionEvent e) {
                                if (selected != null) play(selected);
                            }
                        });
        JScrollPane scroll = Ui.scroll(table);
        int height = 36 + Math.max(1, rows.size()) * 58;
        scroll.setPreferredSize(new Dimension(780, Math.min(430, height)));
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, Math.min(430, height)));
        return scroll;
    }

    private void renderPlaylists(JPanel body) {
        append(body, section(page, Ui.primary("+ 새 플레이리스트", this::create)));
        body.add(Box.createVerticalStrut(10));
        append(
                body,
                Ui.muted(
                        page.equals("내 플레이리스트")
                                ? "내 취향으로 채운 음악들을 한곳에."
                                : "다른 사람의 취향을 발견하고 좋아요로 저장하세요."));
        body.add(Box.createVerticalStrut(25));
        List<Playlist> rows = page.equals("내 플레이리스트") ? mine : shared;
        if (rows.isEmpty()) {
            append(
                    body,
                    empty(
                            "아직 플레이리스트가 없어요",
                            page.equals("내 플레이리스트")
                                    ? "새 플레이리스트를 만들고 좋아하는 음악을 담아보세요."
                                    : "공개한 플레이리스트가 이곳에 모입니다."));
            return;
        }
        JPanel grid = new JPanel(new GridLayout(0, 3, 20, 20));
        for (Playlist p : rows) {
            JPanel card = new JPanel(new BorderLayout(0, 10));
            MusicArt art = new MusicArt(p.id(), 190);
            card.add(art, BorderLayout.CENTER);
            JPanel words = Ui.column();
            words.add(Ui.text(p.title(), 15, true));
            words.add(Box.createVerticalStrut(7));
            words.add(
                    Ui.muted(
                            p.nickname()
                                    + " · "
                                    + (p.isPublic() ? "공개" : "비공개")
                                    + " · 좋아요 "
                                    + p.likes()));
            words.add(
                    Ui.row(
                            Ui.quiet("열기", () -> open(p)),
                            Ui.quiet(p.liked() ? "좋아요 취소" : "좋아요", () -> like(p))));
            if (member != null && p.owner().equals(member.id()))
                words.add(Ui.row(Ui.quiet("수정", () -> edit(p)), Ui.quiet("삭제", () -> delete(p))));
            card.add(words, BorderLayout.SOUTH);
            grid.add(card);
            art.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            art.addMouseListener(
                    new MouseAdapter() {
                        public void mouseClicked(MouseEvent e) {
                            open(p);
                        }
                    });
        }
        int h = ((rows.size() + 2) / 3) * 335;
        grid.setPreferredSize(new Dimension(780, h));
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
        append(body, grid);
    }

    private JPanel empty(String title, String subtitle) {
        JPanel p = Ui.column();
        p.setBorder(BorderFactory.createEmptyBorder(30, 20, 30, 20));
        p.add(Ui.text(title, 17, true));
        p.add(Box.createVerticalStrut(9));
        p.add(Ui.muted(subtitle));
        p.setPreferredSize(new Dimension(700, 120));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 135));
        return p;
    }

    private boolean requireConnection() {
        if (playlists == null || library == null || membership == null) {
            Ui.info(this, "음악 보관함을 불러오지 못했습니다. 잠시 후 다시 실행해 주세요.");
            return false;
        }
        return true;
    }

    private void select(MediaTrack track) {
        selected = track;
        if (!player.playing()) {
            nowTitle.setText(track.song().title());
            nowArtist.setText(track.song().artists());
            nowArt.setIcon(new AlbumArt(track, 48));
            time.setText("0:00 / " + track.song().duration());
            updateFavoriteButton();
        }
    }

    private void play(MediaTrack track) {
        if (!requireConnection()) return;
        if (!track.playable()) {
            Ui.work(
                    this,
                    () ->
                            new OnlineStart(
                                    new OnlineVideoService().resolve(track.song().id()),
                                    membership.status()),
                    data -> {
                        player.stop();
                        selected = track;
                        onlineMode = true;
                        if (onlinePlayer == null) onlinePlayer = new OnlineVideoPanel();
                        pass = data.pass();
                        onlinePlayer.updatePass(pass);
                        onlinePlayer.play(track, data.link());
                        page = "지금 재생 중";
                        nowTitle.setText(track.song().title());
                        nowArtist.setText(track.song().artists());
                        nowArt.setIcon(new AlbumArt(track, 48));
                        limitAnnounced = false;
                        render();
                    });
            return;
        }
        if (onlinePlayer != null) onlinePlayer.pause();
        onlineMode = false;
        Ui.work(
                this,
                () -> library.authorize(track.song().id()),
                authorized -> {
                    selected = authorized.track();
                    pass = authorized.pass();
                    player.play(selected, pass);
                    limitAnnounced = false;
                    nowTitle.setText(selected.song().title());
                    nowArtist.setText(selected.song().artists());
                    nowArt.setIcon(new AlbumArt(track, 48));
                    updatePassLabel();
                    updateFavoriteButton();
                });
    }

    private record OnlineStart(OnlineVideo link, PassStatus pass) {}

    private void attachAudio(MediaTrack track) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("선택한 곡의 보유 MP3 음원 연결");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("MP3 음원", "mp3"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        java.nio.file.Path file = chooser.getSelectedFile().toPath();
        Ui.work(
                this,
                () -> {
                    new LocalAudioService().attach(track.song().id(), file);
                    return true;
                },
                ok -> {
                    refresh();
                    Ui.info(this, "음원을 연결했습니다. 이 곡을 다시 선택하면 재생됩니다.");
                });
    }

    private MediaTrack footerTrack() {
        return player.playing() && player.track() != null ? player.track() : selected;
    }

    private void openAlbum(MediaTrack track) {
        if (track == null) return;
        if (!requireConnection()) return;
        new AlbumDialog(this, track, member, this::refresh, this::play).setVisible(true);
    }

    private void toggleFavorite(MediaTrack track) {
        if (track == null || !requireConnection()) return;
        boolean next = !favoriteIds.contains(track.song().id());
        Ui.work(
                this,
                () -> {
                    new FavoriteService(member).set(track.song().id(), next);
                    return new FavoriteService(member).ids();
                },
                ids -> {
                    favoriteIds = ids;
                    render();
                });
    }

    private void updateFavoriteButton() {
        MediaTrack track = footerTrack();
        favoriteButton.setEnabled(track != null && member != null);
        boolean on = track != null && favoriteIds.contains(track.song().id());
        favoriteButton.setText(on ? "★" : "☆");
        favoriteButton.setForeground(on ? new Color(177, 119, 6) : Ui.MUTED);
    }

    public void playSong(Song song) {
        tracks.stream()
                .filter(t -> t.song().id() == song.id())
                .findFirst()
                .ifPresentOrElse(
                        this::play, () -> Ui.info(this, "이 곡에는 재생 가능한 실제 음원이 연결되어 있지 않습니다."));
    }

    private void skip(int direction) {
        if (tracks.isEmpty()) return;
        int index =
                selected == null
                        ? 0
                        : java.util.stream.IntStream.range(0, tracks.size())
                                .filter(i -> tracks.get(i).song().id() == selected.song().id())
                                .findFirst()
                                .orElse(0);
        play(tracks.get(Math.floorMod(index + direction, tracks.size())));
    }

    private void tick() {
        if (onlineMode && onlinePlayer != null) {
            var state = onlinePlayer.state();
            playButton.setIcon(
                    new MuseIcon(
                            state.present() && !state.paused() ? "pause" : "play", 24, Ui.ACCENT));
            if (!progress.getValueIsAdjusting()) {
                updatingSlider = true;
                progress.setMaximum(Math.max(1, (int) state.duration()));
                progress.setValue((int) state.position());
                updatingSlider = false;
            }
            time.setText(
                    state.present()
                            ? format(state.position()) + " / " + format(state.duration())
                            : "불러오는 중");
            playMode.setText(
                    !onlinePlayer.unlimited() && state.minuteStopped()
                            ? "1분 미리듣기가 끝났어요 · Plus로 전체 듣기"
                            : onlinePlayer.unlimited() ? "Plus · 전체 재생" : "무료 · 1분 미리듣기");
            return;
        }
        playButton.setIcon(
                new MuseIcon(
                        player.playing() && !player.paused() ? "pause" : "play", 24, Ui.ACCENT));
        MediaTrack current = player.track();
        if (current != null) {
            if (!progress.getValueIsAdjusting()) {
                updatingSlider = true;
                progress.setMaximum(player.limit());
                progress.setValue((int) player.position());
                updatingSlider = false;
            }
            time.setText(format(player.position()) + " / " + current.song().duration());
        }
        if (player.limited() && !limitAnnounced) {
            limitAnnounced = true;
            playMode.setText("1분 미리듣기가 끝났어요 · Plus로 전체 듣기");
        }
        if (player.error() != null) playMode.setText(player.error());
    }

    private String format(double seconds) {
        int s = (int) seconds;
        return (s / 60) + ":" + String.format("%02d", s % 60);
    }

    private void updatePassLabel() {
        playMode.setText(pass.active() ? "Plus · 전체 재생" : "무료 · 1분 미리듣기");
        planButton.setText(pass.active() ? "Plus 이용 중" : "Plus 이용권");
    }

    private void openMembership() {
        if (!requireConnection()) return;
        if (onlinePlayer != null) onlinePlayer.pause();
        new MembershipDialog(
                        this,
                        membership,
                        () -> {
                            player.stop();
                            refresh();
                        })
                .setVisible(true);
    }

    private void create() {
        if (!requireConnection()) return;
        PlaylistEditor.open(
                this,
                null,
                v ->
                        Ui.work(
                                this,
                                () -> playlists.create(v.title(), v.description(), v.isPublic()),
                                id -> {
                                    page = "내 플레이리스트";
                                    refresh();
                                }));
    }

    private void open(Playlist p) {
        if (!requireConnection()) return;
        new DetailDialog(this, playlists, p.id(), this::refresh, this::playSong).setVisible(true);
    }

    private void edit(Playlist p) {
        if (!requireConnection()) return;
        PlaylistEditor.open(
                this,
                p,
                v ->
                        Ui.work(
                                this,
                                () -> {
                                    playlists.edit(
                                            p.id(), v.title(), v.description(), v.isPublic());
                                    return true;
                                },
                                ok -> refresh()));
    }

    private void delete(Playlist p) {
        if (!requireConnection()) return;
        if (JOptionPane.showConfirmDialog(
                        this,
                        "‘" + p.title() + "’ 플레이리스트를 삭제할까요? 곡 원본은 유지됩니다.",
                        "플레이리스트 삭제",
                        JOptionPane.YES_NO_OPTION)
                != JOptionPane.YES_OPTION) return;
        Ui.work(
                this,
                () -> {
                    playlists.delete(p.id());
                    return true;
                },
                ok -> refresh());
    }

    private void like(Playlist p) {
        if (!requireConnection()) return;
        Ui.work(
                this,
                () -> {
                    playlists.like(p.id(), !p.liked());
                    return true;
                },
                ok -> refresh());
    }

    private void addSelected() {
        if (selected == null) {
            Ui.info(this, "먼저 담을 곡을 선택해 주세요.");
            return;
        }
        if (!requireConnection()) return;
        if (mine.isEmpty()) {
            Ui.info(this, "곡을 담을 플레이리스트를 먼저 만들어 주세요.");
            create();
            return;
        }
        String[] names =
                mine.stream().map(p -> p.title() + "  (#" + p.id() + ")").toArray(String[]::new);
        String chosen =
                (String)
                        JOptionPane.showInputDialog(
                                this,
                                "곡을 담을 플레이리스트를 선택하세요.",
                                "플레이리스트에 담기",
                                JOptionPane.PLAIN_MESSAGE,
                                null,
                                names,
                                names[0]);
        if (chosen == null) return;
        int index = Arrays.asList(names).indexOf(chosen);
        long playlist = mine.get(index).id(), song = selected.song().id();
        Ui.work(
                this,
                () -> {
                    playlists.add(playlist, song);
                    return true;
                },
                ok -> {
                    Ui.info(this, "플레이리스트에 곡을 담았습니다.");
                    refresh();
                });
    }
}
