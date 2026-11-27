package kr.ac.yumin.playlist.ui;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/** Loopback-only host supplies the normal origin/referrer required by YouTube embeds. */
final class YouTubePlayerPage implements AutoCloseable {
    private final HttpServer server;

    YouTubePlayerPage() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/player",
                exchange -> {
                    if (!exchange.getRequestMethod().equals("GET")) {
                        exchange.sendResponseHeaders(405, -1);
                        exchange.close();
                        return;
                    }
                    byte[] body = HTML.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                    exchange.getResponseHeaders().set("Cache-Control", "no-store");
                    exchange.getResponseHeaders()
                            .set("Referrer-Policy", "strict-origin-when-cross-origin");
                    exchange.sendResponseHeaders(200, body.length);
                    try (var out = exchange.getResponseBody()) {
                        out.write(body);
                    }
                });
        server.setExecutor(
                Executors.newSingleThreadExecutor(
                        r -> {
                            Thread t = new Thread(r, "muse-player-page");
                            t.setDaemon(true);
                            return t;
                        }));
        server.start();
    }

    String url(String id, long generation, long premiumMillis, float volume, boolean muted) {
        return "http://127.0.0.1:"
                + server.getAddress().getPort()
                + "/player?id="
                + id
                + "&generation="
                + generation
                + "&premium="
                + premiumMillis
                + "&volume="
                + volume
                + "&muted="
                + muted;
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private static final String HTML =
            """
<!doctype html><html lang="ko"><head><meta charset="utf-8">
<meta name="referrer" content="strict-origin-when-cross-origin">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>muse · 공식 뮤직비디오</title>
<style>html,body{margin:0;width:100%;height:100%;background:#111;overflow:hidden}#player{width:100%;height:100%;border:0}</style>
</head><body><div id="player"></div><script>
const params=new URLSearchParams(location.search),videoId=params.get('id');
window.musePremiumUntil=performance.now()+Number(params.get('premium'));window.museMinuteStopped=false;
window.museSetPass=ms=>{window.musePremiumUntil=performance.now()+ms;window.museMinuteStopped=false;};
window.museReady=false;window.museError=0;
function onYouTubeIframeAPIReady(){
  window.musePlayer=new YT.Player('player',{videoId:videoId,
    playerVars:{autoplay:1,controls:1,playsinline:1,rel:0,origin:location.origin},
    events:{onReady:e=>{window.museReady=true;e.target.setVolume(Number(params.get('volume'))*100);
      if(params.get('muted')==='true')e.target.mute();e.target.playVideo();},
      onError:e=>{window.museError=e.data;}}});
}
setInterval(()=>{
  const p=window.musePlayer,ready=window.museReady&&p&&p.getCurrentTime;
  let time=ready?p.getCurrentTime():0,duration=ready?p.getDuration():0,s=ready?p.getPlayerState():-1;
  let limited=performance.now()>=window.musePremiumUntil&&time>=60;
  if(ready&&limited){
    window.museMinuteStopped=true;if(s===1){p.pauseVideo();s=2;}
  }
  if(window.cefQuery)window.cefQuery({request:JSON.stringify({generation:Number(params.get('generation')),
    url:'https://www.youtube.com/watch?v='+videoId,present:!!ready,time:time,duration:duration,
    paused:s!==1,ad:false,minuteStopped:window.museMinuteStopped,error:window.museError})});
},100);
</script><script src="https://www.youtube.com/iframe_api"></script></body></html>
""";
}
