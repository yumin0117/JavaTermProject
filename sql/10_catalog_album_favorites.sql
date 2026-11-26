-- Additive extension: music catalog, album facts, song credits, member favorites.
SET DEFINE OFF;
DECLARE
  n NUMBER;
BEGIN
  SELECT COUNT(*) INTO n FROM user_tables WHERE table_name='MUSIC_CATALOG';
  IF n=0 THEN EXECUTE IMMEDIATE 'CREATE TABLE MUSIC_CATALOG (
    song_id NUMBER(10) CONSTRAINT pk_music_catalog PRIMARY KEY,
    melon_song_id NUMBER(10) NOT NULL,
    cover_url VARCHAR2(1000 CHAR),
    added_at TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT fk_catalog_song FOREIGN KEY(song_id) REFERENCES SONG(song_id))'; END IF;
  SELECT COUNT(*) INTO n FROM user_tables WHERE table_name='ALBUM_INFO';
  IF n=0 THEN EXECUTE IMMEDIATE 'CREATE TABLE ALBUM_INFO (
    album_id NUMBER(10) CONSTRAINT pk_album_info PRIMARY KEY,
    album_title VARCHAR2(200 CHAR) NOT NULL,
    album_artists VARCHAR2(1000 CHAR),
    release_date DATE,
    album_type VARCHAR2(80 CHAR),
    genre VARCHAR2(200 CHAR),
    distributor VARCHAR2(500 CHAR),
    agency VARCHAR2(500 CHAR),
    cover_url VARCHAR2(1000 CHAR),
    source_url VARCHAR2(1000 CHAR) NOT NULL,
    fetched_at TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL)'; END IF;
  SELECT COUNT(*) INTO n FROM user_tables WHERE table_name='SONG_DETAIL';
  IF n=0 THEN EXECUTE IMMEDIATE 'CREATE TABLE SONG_DETAIL (
    song_id NUMBER(10) CONSTRAINT pk_song_detail PRIMARY KEY,
    album_id NUMBER(10) NOT NULL,
    source_url VARCHAR2(1000 CHAR) NOT NULL,
    fetched_at TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT fk_detail_song FOREIGN KEY(song_id) REFERENCES SONG(song_id),
    CONSTRAINT fk_detail_album FOREIGN KEY(album_id) REFERENCES ALBUM_INFO(album_id))'; END IF;
  SELECT COUNT(*) INTO n FROM user_tables WHERE table_name='SONG_CREDIT';
  IF n=0 THEN EXECUTE IMMEDIATE 'CREATE TABLE SONG_CREDIT (
    song_id NUMBER(10) NOT NULL,
    credit_role VARCHAR2(10 CHAR) NOT NULL CHECK(credit_role IN (''작사'',''작곡'',''편곡'')),
    credit_order NUMBER(3) NOT NULL CHECK(credit_order>0),
    person_name VARCHAR2(200 CHAR) NOT NULL,
    CONSTRAINT pk_song_credit PRIMARY KEY(song_id,credit_role,credit_order),
    CONSTRAINT fk_credit_song FOREIGN KEY(song_id) REFERENCES SONG(song_id))'; END IF;
  SELECT COUNT(*) INTO n FROM user_tables WHERE table_name='SONG_FAVORITE';
  IF n=0 THEN EXECUTE IMMEDIATE 'CREATE TABLE SONG_FAVORITE (
    member_id VARCHAR2(30 CHAR) NOT NULL,
    song_id NUMBER(10) NOT NULL,
    created_at TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_song_favorite PRIMARY KEY(member_id,song_id),
    CONSTRAINT fk_fav_member FOREIGN KEY(member_id) REFERENCES MEMBER(member_id),
    CONSTRAINT fk_fav_song FOREIGN KEY(song_id) REFERENCES SONG(song_id))'; END IF;
END;
/
MERGE INTO MUSIC_CATALOG d USING(SELECT song_id,melon_song_id,cover_url FROM MELON_CHART_ENTRY WHERE chart_at=(SELECT MAX(chart_at) FROM MELON_CHART)) x ON(d.song_id=x.song_id) WHEN NOT MATCHED THEN INSERT(song_id,melon_song_id,cover_url) VALUES(x.song_id,x.melon_song_id,x.cover_url);
COMMIT;
