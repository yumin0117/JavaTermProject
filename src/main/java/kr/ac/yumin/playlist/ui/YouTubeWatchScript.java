package kr.ac.yumin.playlist.ui;

/** Controls the complete official watch page. Its own fullscreen UI shows the video in the app. */
final class YouTubeWatchScript {
    private YouTubeWatchScript() {}

    static String build(
            long generation, String videoId, long premiumMillis, float volume, boolean muted) {
        return SCRIPT.replace("GENERATION", Long.toString(generation))
                .replace("VIDEO_ID", videoId)
                .replace("PREMIUM_MS", Long.toString(premiumMillis))
                .replace("VOLUME_VALUE", Float.toString(volume))
                .replace("TEST_MUTED", Boolean.toString(muted));
    }

    private static final String SCRIPT =
            """
(()=>{
  if(window.museWatchTimer)clearInterval(window.museWatchTimer);
  window.musePremiumUntil=performance.now()+PREMIUM_MS;window.museMinuteStopped=false;
  window.museSetPass=ms=>{window.musePremiumUntil=performance.now()+ms;window.museMinuteStopped=false;};
  let attached=null,lastTime=0,lastDuration=0;
  const video=()=>document.querySelector('video');
  const ad=()=>!!document.querySelector('.html5-video-player.ad-showing');
  window.museReady=true;
  window.musePlayer={
    getCurrentTime:()=>ad()?lastTime:(video()?.currentTime||0),getDuration:()=>ad()?lastDuration:(video()?.duration||0),
    getPlayerState:()=>video()&&!video().paused?1:2,
    pauseVideo:()=>video()?.pause(),playVideo:()=>video()?.play()?.catch(()=>{}),
    seekTo:t=>{if(video()&&!ad())video().currentTime=t;},
    setVolume:n=>{if(video())video().volume=Math.max(0,Math.min(1,n/100));},
    mute:()=>{if(video())video().muted=true;},unMute:()=>{if(video())video().muted=false;}
  };
  window.museWatchTimer=setInterval(()=>{
    const v=video(),advert=ad(),player=document.getElementById('movie_player');
  const playingId=player?.getVideoData?.()?.video_id;
  const sameVideo=playingId==='VIDEO_ID'&&new URL(location.href).searchParams.get('v')==='VIDEO_ID';
    if(v&&attached!==v){attached=v;v.volume=VOLUME_VALUE;if(TEST_MUTED)v.muted=true;}
    if(v&&!advert&&playingId&&playingId!=='VIDEO_ID')v.pause();
    if(v&&!advert&&sameVideo){lastTime=Number.isFinite(v.currentTime)?v.currentTime:0;lastDuration=Number.isFinite(v.duration)?v.duration:0;}
    if(v&&!advert&&sameVideo&&performance.now()>=window.musePremiumUntil&&lastTime>=60){v.pause();window.museMinuteStopped=true;}
    const b=document.querySelector('.ytp-fullscreen-button'),r=b?.getBoundingClientRect();
    window.cefQuery&&window.cefQuery({request:JSON.stringify({generation:GENERATION,url:location.href,
      present:!!v&&sameVideo,time:lastTime,duration:lastDuration,paused:!v||v.paused,ad:advert,
      minuteStopped:window.museMinuteStopped,error:0,fullscreen:!!document.fullscreenElement,
      fullscreenX:r&&r.width>0?r.x+r.width/2:0,fullscreenY:r&&r.height>0?r.y+r.height/2:0})});
  },100);
})();
""";
}
