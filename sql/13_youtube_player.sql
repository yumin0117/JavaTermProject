-- Official YouTube playback links only. No audio files or streaming URLs are extracted.
SET DEFINE OFF;
DECLARE
  n NUMBER;
BEGIN
  SELECT COUNT(*) INTO n FROM user_tables WHERE table_name='SONG_VIDEO';
  IF n=0 THEN EXECUTE IMMEDIATE 'CREATE TABLE SONG_VIDEO (
    song_id NUMBER(10) CONSTRAINT pk_song_video PRIMARY KEY,
    youtube_video_id VARCHAR2(11 CHAR),
    youtube_title VARCHAR2(500 CHAR),
    channel_name VARCHAR2(255 CHAR),
    video_kind VARCHAR2(12 CHAR) DEFAULT ''SEARCH'' NOT NULL,
    search_query VARCHAR2(1000 CHAR) NOT NULL,
    match_status VARCHAR2(20 CHAR) DEFAULT ''SEARCH'' NOT NULL,
    checked_at TIMESTAMP(6),
    CONSTRAINT fk_video_song FOREIGN KEY(song_id) REFERENCES SONG(song_id),
    CONSTRAINT ck_video_kind CHECK(video_kind IN (''MV'',''AUDIO'',''LIVE'',''SEARCH'')),
    CONSTRAINT ck_video_status CHECK(match_status IN (''SEARCH'',''METADATA'',''PLAYABLE'',''UNAVAILABLE'')))'; END IF;
END;
/
MERGE INTO SONG_VIDEO d USING(SELECT s.song_id,s.title||' '||(SELECT LISTAGG(a.artist_name,' ') WITHIN GROUP(ORDER BY a.artist_id) FROM SONG_ARTIST sa JOIN ARTIST a ON a.artist_id=sa.artist_id WHERE sa.song_id=s.song_id)||' official music video' q FROM SONG s JOIN MUSIC_CATALOG cat ON cat.song_id=s.song_id) x ON(d.song_id=x.song_id) WHEN NOT MATCHED THEN INSERT(song_id,search_query) VALUES(x.song_id,x.q);
COMMIT;
