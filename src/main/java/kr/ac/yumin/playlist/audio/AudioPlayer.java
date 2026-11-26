package kr.ac.yumin.playlist.audio;

import kr.ac.yumin.playlist.model.*;

import java.io.*;
import java.util.concurrent.*;

import javax.sound.sampled.*;

/** 실제 MP3를 PCM으로 디코딩하여 사운드 장치로 출력한다. 제한은 UI 타이머가 아닌 출력 프레임에서 적용한다. */
public final class AudioPlayer implements AutoCloseable {
    private final ExecutorService worker =
            Executors.newSingleThreadExecutor(
                    r -> {
                        Thread t = new Thread(r, "muse-audio");
                        t.setDaemon(true);
                        return t;
                    });
    private volatile long generation;
    private volatile boolean paused, playing, limited;
    private volatile double position, seek = -1;
    private volatile float volume = .65f;
    private volatile String error;
    private volatile SourceDataLine line;
    private volatile PlaybackPolicy policy;
    private volatile MediaTrack track;

    public synchronized void play(MediaTrack next, PassStatus pass) {
        stop();
        track = next;
        policy = new PlaybackPolicy(pass);
        position = 0;
        seek = 0;
        error = null;
        limited = false;
        paused = false;
        playing = true;
        long ticket = generation;
        worker.submit(() -> decode(ticket, next));
    }

    public void updatePass(PassStatus pass) {
        PlaybackPolicy current = policy;
        if (current != null) current.update(pass);
    }

    public void togglePause() {
        if (playing) paused = !paused;
    }

    public void seek(double seconds) {
        PlaybackPolicy p = policy;
        MediaTrack t = track;
        if (p != null && t != null && playing) {
            seek = p.clampSeek(seconds, t.song().seconds());
            limited = false;
        }
    }

    public void volume(float value) {
        volume = Math.max(0, Math.min(1, value));
    }

    public double position() {
        return position;
    }

    public boolean playing() {
        return playing;
    }

    public boolean paused() {
        return paused;
    }

    public boolean limited() {
        return limited;
    }

    public String error() {
        return error;
    }

    public MediaTrack track() {
        return track;
    }

    public int limit() {
        return track == null || policy == null ? 60 : policy.limit(track.song().seconds());
    }

    public synchronized void stop() {
        generation++;
        playing = false;
        paused = false;
        SourceDataLine out = line;
        if (out != null) {
            out.stop();
            out.flush();
            out.close();
        }
        line = null;
    }

    private AudioInputStream pcm(MediaTrack t) throws Exception {
        InputStream resource;
        if (t.resource().matches("local-audio/[a-f0-9-]+[.]mp3")) {
            resource =
                    java.nio.file.Files.newInputStream(
                            java.nio.file.Path.of(
                                    "config/audio",
                                    t.resource().substring("local-audio/".length())));
        } else {
            resource = AudioPlayer.class.getClassLoader().getResourceAsStream(t.resource());
        }
        if (resource == null) throw new FileNotFoundException("음원 파일을 찾을 수 없습니다.");
        AudioInputStream source;
        try {
            source = AudioSystem.getAudioInputStream(new BufferedInputStream(resource));
        } catch (Exception e) {
            resource.close();
            throw e;
        }
        AudioFormat f = source.getFormat();
        AudioFormat decoded =
                new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        f.getSampleRate(),
                        16,
                        f.getChannels(),
                        f.getChannels() * 2,
                        f.getSampleRate(),
                        false);
        try {
            return AudioSystem.getAudioInputStream(decoded, source);
        } catch (Exception e) {
            source.close();
            throw e;
        }
    }

    private void decode(long ticket, MediaTrack selected) {
        try {
            while (ticket == generation) {
                double start = seek >= 0 ? seek : position;
                seek = -1;
                try (AudioInputStream input = pcm(selected)) {
                    AudioFormat format = input.getFormat();
                    int frameSize = format.getFrameSize();
                    double rate = format.getSampleRate();
                    long base = (long) (start * rate), skip = base * frameSize;
                    byte[] buffer = new byte[4096 - (4096 % frameSize)];
                    while (skip > 0 && ticket == generation) {
                        int n = input.read(buffer, 0, (int) Math.min(buffer.length, skip));
                        if (n < 0) {
                            skip = 0;
                            break;
                        }
                        skip -= n;
                    }
                    if (ticket != generation) return;
                    SourceDataLine output = AudioSystem.getSourceDataLine(format);
                    output.open(format, Math.max(buffer.length, frameSize * (int) (rate / 8)));
                    line = output;
                    output.start();
                    long written = base;
                    boolean restart = false;
                    try {
                        while (ticket == generation) {
                            position = (base + output.getLongFramePosition()) / rate;
                            if (seek >= 0) {
                                restart = true;
                                output.flush();
                                break;
                            }
                            if (paused) {
                                output.stop();
                                Thread.sleep(25);
                                continue;
                            } else if (!output.isRunning()) output.start();
                            long ceiling = (long) (policy.limit(selected.song().seconds()) * rate);
                            if (written >= ceiling) {
                                output.drain();
                                position = Math.min(written, ceiling) / rate;
                                limited =
                                        policy.limit(selected.song().seconds())
                                                < selected.song().seconds();
                                break;
                            }
                            int wanted =
                                    (int) Math.min(buffer.length, (ceiling - written) * frameSize);
                            int n = input.read(buffer, 0, wanted);
                            if (n < 0) {
                                output.drain();
                                position = written / rate;
                                break;
                            }
                            scale(buffer, n, volume);
                            int offset = 0;
                            while (offset < n && ticket == generation) {
                                int count = output.write(buffer, offset, n - offset);
                                if (count <= 0) break;
                                offset += count;
                            }
                            written += offset / frameSize;
                        }
                    } finally {
                        output.stop();
                        output.close();
                        if (line == output) line = null;
                    }
                    if (!restart) break;
                }
            }
        } catch (Exception failure) {
            if (ticket == generation) error = "음원을 재생할 수 없습니다. 기본 오디오 출력 장치와 음원 파일을 확인해 주세요.";
        } finally {
            if (ticket == generation) playing = false;
        }
    }

    private static void scale(byte[] bytes, int length, float volume) {
        for (int i = 0; i + 1 < length; i += 2) {
            short original = (short) ((bytes[i] & 255) | (bytes[i + 1] << 8));
            short value = (short) (original * volume);
            bytes[i] = (byte) value;
            bytes[i + 1] = (byte) (value >> 8);
        }
    }

    @Override
    public void close() {
        stop();
        worker.shutdownNow();
    }
}
