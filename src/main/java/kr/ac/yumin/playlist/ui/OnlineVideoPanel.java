package kr.ac.yumin.playlist.ui;

import com.google.gson.*;

import kr.ac.yumin.playlist.model.*;
import kr.ac.yumin.playlist.service.*;

import me.friwi.jcefmaven.*;

import org.cef.*;
import org.cef.browser.*;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.*;

import java.awt.*;
import java.io.File;
import java.util.concurrent.*;

import javax.swing.*;

/** Standard visible YouTube embed, controlled through the documented IFrame API. */
public final class OnlineVideoPanel extends JPanel implements AutoCloseable {
    public record State(
            double position,
            double duration,
            boolean paused,
            boolean ad,
            boolean present,
            boolean minuteStopped) {}

    private static final ExecutorService WORK =
            Executors.newSingleThreadExecutor(
                    r -> {
                        Thread t = new Thread(r, "muse-online");
                        t.setDaemon(true);
                        return t;
                    });
    private static CompletableFuture<CefApp> engine;
    private final JPanel surface = new JPanel(new BorderLayout());
    private final JLabel heading = Ui.text("공식 뮤직비디오", 16, true),
            status = Ui.muted("YouTube 공식 플레이어");
    private volatile State state = new State(0, 0, true, false, false, false);
    private volatile long generation;
    private volatile int reportedError;
    private CefClient client;
    private CefBrowser browser;
    private YouTubePlayerPage page;
    private OnlineVideo link;
    private MediaTrack track;
    private boolean disposed, initializing, mutedForTest;
    private volatile boolean playbackRequested;
    private float volume = .65f;
    private PassStatus pass = PassStatus.free();
    private long premiumDeadline;

    public OnlineVideoPanel() {
        super(new BorderLayout());
        JPanel header = new JPanel(new BorderLayout(12, 6));
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 12, 20));
        header.add(heading, BorderLayout.CENTER);
        header.add(Ui.row(Ui.quiet("영상 변경", this::changeVideo)), BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        surface.setBackground(Color.BLACK);
        showMessage("곡을 선택하면 공식 영상이 앱 안에서 열립니다.");
        add(surface, BorderLayout.CENTER);
        status.setBorder(BorderFactory.createEmptyBorder(9, 20, 10, 20));
        add(status, BorderLayout.SOUTH);
    }

    private static synchronized CompletableFuture<CefApp> engine() {
        if (engine == null)
            engine =
                    CompletableFuture.supplyAsync(
                            () -> {
                                try {
                                    CefAppBuilder builder = new CefAppBuilder();
                                    builder.setInstallDir(new File(".runtime/jcef"));
                                    builder.getCefSettings().windowless_rendering_enabled = false;
                                    builder.getCefSettings().cache_path =
                                            new File(
                                                            System.getProperty(
                                                                    "muse.browser.profile",
                                                                    ".runtime/youtube-embed-profile"))
                                                    .getAbsolutePath();
                                    builder.getCefSettings().log_severity =
                                            org.cef.CefSettings.LogSeverity.LOGSEVERITY_WARNING;
                                    builder.addJcefArgs(
                                            "--autoplay-policy=no-user-gesture-required",
                                            "--lang=ko-KR");
                                    return builder.build();
                                } catch (Exception e) {
                                    throw new CompletionException(e);
                                }
                            },
                            WORK);
        return engine;
    }

    public void play(MediaTrack track, OnlineVideo link) {
        playbackRequested = true;
        this.track = track;
        this.link = link;
        generation++;
        reportedError = 0;
        state = new State(0, 0, true, false, false, false);
        heading.setText(track.song().title() + " · " + track.song().artists());
        status.setText(
                link.kindLabel() + " · " + (pass.active() ? "Plus · 전체 재생" : "무료 · 1분 미리듣기"));
        if (!link.hasVideo()) {
            if (browser != null) browser.loadURL("about:blank");
            showMessage("이 곡의 공식 영상은 아직 연결되지 않았습니다. ‘영상 변경’에서 주소를 연결할 수 있습니다.");
            return;
        }
        try {
            if (page == null) page = new YouTubePlayerPage();
        } catch (Exception e) {
            status.setText("앱 내부 플레이어를 시작하지 못했습니다. 앱을 다시 실행해 주세요.");
            return;
        }
        if (browser != null) {
            showBrowser();
            browser.loadURL(playerUrl());
            return;
        }
        if (initializing) return;
        initializing = true;
        showMessage("온라인 플레이어를 준비하고 있습니다. 처음 실행할 때는 잠시 걸릴 수 있습니다.");
        engine().whenComplete(
                        (app, error) ->
                                SwingUtilities.invokeLater(
                                        () -> {
                                            initializing = false;
                                            if (disposed) return;
                                            if (error != null) {
                                                status.setText(
                                                        "온라인 플레이어를 준비하지 못했습니다. 인터넷 연결을 확인해 주세요.");
                                                return;
                                            }
                                            client = app.createClient();
                                            CefMessageRouter router = CefMessageRouter.create();
                                            router.addHandler(
                                                    new CefMessageRouterHandlerAdapter() {
                                                        @Override
                                                        public boolean onQuery(
                                                                CefBrowser b,
                                                                CefFrame frame,
                                                                long id,
                                                                String request,
                                                                boolean persistent,
                                                                CefQueryCallback callback) {
                                                            try {
                                                                if (page == null
                                                                        || !b.getURL()
                                                                                .startsWith(
                                                                                        "http://127.0.0.1:")) {
                                                                    callback.failure(
                                                                            1,
                                                                            "Unexpected player"
                                                                                    + " origin");
                                                                    return true;
                                                                }
                                                                JsonObject data =
                                                                        JsonParser.parseString(
                                                                                        request)
                                                                                .getAsJsonObject();
                                                                if (data.get("generation")
                                                                                .getAsLong()
                                                                        != generation) {
                                                                    callback.success("");
                                                                    return true;
                                                                }
                                                                state =
                                                                        new State(
                                                                                data.get("time")
                                                                                        .getAsDouble(),
                                                                                data.get("duration")
                                                                                        .getAsDouble(),
                                                                                data.get("paused")
                                                                                        .getAsBoolean(),
                                                                                false,
                                                                                data.get("present")
                                                                                        .getAsBoolean(),
                                                                                data.get(
                                                                                                "minuteStopped")
                                                                                        .getAsBoolean());
                                                                if (!playbackRequested
                                                                        && !state.paused())
                                                                    SwingUtilities.invokeLater(
                                                                            OnlineVideoPanel.this
                                                                                    ::pause);
                                                                int code =
                                                                        data.get("error")
                                                                                .getAsInt();
                                                                if (code != 0
                                                                        && code != reportedError) {
                                                                    reportedError = code;
                                                                    SwingUtilities.invokeLater(
                                                                            () ->
                                                                                    status.setText(
                                                                                            "앱 내 재생을"
                                                                                                + " 사용할 수"
                                                                                                + " 없는 영상입니다"
                                                                                                + " ("
                                                                                                    + code
                                                                                                    + "). ‘영상"
                                                                                                    + " 변경’으로"
                                                                                                    + " 다른 공식"
                                                                                                    + " 영상을 연결해"
                                                                                                    + " 주세요."));
                                                                }
                                                                callback.success("");
                                                                return true;
                                                            } catch (Exception e) {
                                                                callback.failure(
                                                                        1, "Invalid player state");
                                                                return true;
                                                            }
                                                        }
                                                    },
                                                    true);
                                            client.addMessageRouter(router);
                                            client.addLoadHandler(
                                                    new CefLoadHandlerAdapter() {
                                                        @Override
                                                        public void onLoadError(
                                                                CefBrowser b,
                                                                CefFrame frame,
                                                                ErrorCode code,
                                                                String text,
                                                                String url) {
                                                            if (frame.isMain()
                                                                    && code
                                                                            != ErrorCode
                                                                                    .ERR_ABORTED)
                                                                SwingUtilities.invokeLater(
                                                                        () ->
                                                                                status.setText(
                                                                                        "영상을 불러오지"
                                                                                            + " 못했습니다."
                                                                                            + " 인터넷 연결을"
                                                                                            + " 확인해 주세요."));
                                                        }
                                                    });
                                            if (this.link == null || !this.link.hasVideo()) return;
                                            browser =
                                                    client.createBrowser(playerUrl(), false, false);
                                            showBrowser();
                                        }));
    }

    private long premiumMillis() {
        return pass.active()
                ? Math.max(
                        0,
                        java.time.Duration.between(pass.databaseNow(), pass.expiresAt()).toMillis())
                : 0;
    }

    public void updatePass(PassStatus pass) {
        this.pass = pass;
        premiumDeadline =
                System.nanoTime()
                        + java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(premiumMillis());
        execute("window.museSetPass&&window.museSetPass(" + premiumMillis() + ");");
        if (link != null)
            status.setText(
                    link.kindLabel() + " · " + (pass.active() ? "Plus · 전체 재생" : "무료 · 1분 미리듣기"));
    }

    public boolean unlimited() {
        return System.nanoTime() < premiumDeadline;
    }

    private String playerUrl() {
        return page.url(
                link.videoId(),
                generation,
                Math.max(
                        0,
                        java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(
                                premiumDeadline - System.nanoTime())),
                volume,
                mutedForTest);
    }

    private void showMessage(String text) {
        surface.removeAll();
        JLabel message = Ui.muted(text);
        message.setHorizontalAlignment(SwingConstants.CENTER);
        surface.add(message, BorderLayout.CENTER);
        surface.revalidate();
        surface.repaint();
    }

    private void showBrowser() {
        surface.removeAll();
        surface.add(browser.getUIComponent(), BorderLayout.CENTER);
        surface.revalidate();
        surface.repaint();
    }

    public State state() {
        return state;
    }

    public int playerError() {
        return reportedError;
    }

    public OnlineVideo link() {
        return link;
    }

    public void mutedForTest(boolean muted) {
        mutedForTest = muted;
        execute("if(window.museReady){window.musePlayer." + (muted ? "mute" : "unMute") + "();}");
    }

    public void pause() {
        playbackRequested = false;
        State before = state;
        state =
                new State(
                        before.position(),
                        before.duration(),
                        true,
                        before.ad(),
                        before.present(),
                        before.minuteStopped());
        execute("if(window.museReady)window.musePlayer.pauseVideo();");
    }

    public void togglePause() {
        playbackRequested = state.paused();
        execute(
                "if(window.museReady){let"
                    + " p=window.musePlayer;if(p.getPlayerState()===1)p.pauseVideo();else"
                    + " if(performance.now()<window.musePremiumUntil||p.getCurrentTime()<60)p.playVideo();}");
    }

    public void seek(double seconds) {
        execute(
                "if(window.museReady)window.musePlayer.seekTo(Math.min("
                        + Math.max(0, Math.min(seconds, state.duration()))
                        + ",performance.now()<window.musePremiumUntil?Infinity:60),true);");
    }

    public void volume(float v) {
        volume = Math.max(0, Math.min(1, v));
        execute("if(window.museReady)window.musePlayer.setVolume(" + (volume * 100) + ");");
    }

    private void execute(String script) {
        if (browser != null)
            browser.executeJavaScript("(()=>{" + script + "})()", browser.getURL(), 0);
    }

    private void changeVideo() {
        if (link == null || track == null) return;
        String url =
                JOptionPane.showInputDialog(
                        this,
                        "이 곡의 공식 YouTube 영상 주소를 입력해 주세요.",
                        link.hasVideo() ? "https://www.youtube.com/watch?v=" + link.videoId() : "");
        if (url == null || url.isBlank()) return;
        String id = OnlineVideoService.videoId(url.strip());
        if (id == null) {
            Ui.info(this, "https://www.youtube.com/watch?v=영상번호 형식의 주소를 입력해 주세요.");
            return;
        }
        long songId = track.song().id();
        MediaTrack chosen = track;
        Ui.work(
                this,
                () -> new OnlineVideoService().metadata(id),
                metadata -> {
                    int choice =
                            JOptionPane.showConfirmDialog(
                                    this,
                                    "선택 곡: "
                                            + chosen.song().title()
                                            + "\n영상: "
                                            + metadata.title()
                                            + "\n채널: "
                                            + metadata.author()
                                            + "\n\n이 곡의 재생 영상으로 연결할까요?",
                                    "재생 영상 연결",
                                    JOptionPane.YES_NO_OPTION);
                    if (choice != JOptionPane.YES_OPTION) return;
                    String kind =
                            metadata.title()
                                            .toLowerCase()
                                            .matches(".*(official.*m/?v|music video|뮤직비디오).*")
                                    ? "MV"
                                    : "AUDIO";
                    Ui.work(
                            this,
                            () -> {
                                new OnlineVideoService().save(songId, metadata, kind);
                                return new OnlineVideoService().resolve(songId);
                            },
                            saved -> play(chosen, saved));
                });
    }

    @Override
    public void close() {
        disposed = true;
        if (browser != null) browser.close(true);
        if (client != null) client.dispose();
        if (page != null) page.close();
        if (engine != null) engine.thenAccept(CefApp::dispose);
    }
}
