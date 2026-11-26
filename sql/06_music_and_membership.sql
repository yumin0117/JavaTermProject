-- 기존 7개 테이블과 예시 데이터는 보존한다. SQL Developer에서 F5로 실행.
SET DEFINE OFF;
DECLARE
  n NUMBER;
BEGIN
  SELECT COUNT(*) INTO n FROM user_tables WHERE table_name='SONG_MEDIA';
  IF n=0 THEN EXECUTE IMMEDIATE 'CREATE TABLE SONG_MEDIA (
    song_id NUMBER(10) CONSTRAINT pk_song_media PRIMARY KEY,
    resource_path VARCHAR2(255 CHAR) NOT NULL,
    performer VARCHAR2(120 CHAR) NOT NULL,
    source_url VARCHAR2(1000 CHAR) NOT NULL,
    license_name VARCHAR2(120 CHAR) NOT NULL,
    CONSTRAINT fk_media_song FOREIGN KEY(song_id) REFERENCES SONG(song_id))'; END IF;
  SELECT COUNT(*) INTO n FROM user_tables WHERE table_name='MEMBERSHIP_ORDER';
  IF n=0 THEN EXECUTE IMMEDIATE 'CREATE TABLE MEMBERSHIP_ORDER (
    order_id VARCHAR2(36 CHAR) CONSTRAINT pk_membership_order PRIMARY KEY,
    member_id VARCHAR2(30 CHAR) NOT NULL,
    amount_won NUMBER(8) NOT NULL,
    paid_at TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
    valid_until TIMESTAMP(6) NOT NULL,
    status VARCHAR2(10 CHAR) DEFAULT ''PAID'' NOT NULL,
    payment_method VARCHAR2(10 CHAR) DEFAULT ''DEMO'' NOT NULL,
    CONSTRAINT fk_order_member FOREIGN KEY(member_id) REFERENCES MEMBER(member_id),
    CONSTRAINT ck_order_status CHECK(status IN (''PAID'',''CANCELLED'')),
    CONSTRAINT ck_order_demo CHECK(payment_method=''DEMO''),
    CONSTRAINT ck_order_amount CHECK(amount_won=4900))'; END IF;
END;
/

MERGE INTO ARTIST d USING (SELECT 9001 id FROM DUAL) x ON (d.artist_id=x.id) WHEN NOT MATCHED THEN INSERT(artist_id,artist_name,country) VALUES(9001,'Frédéric Chopin','폴란드');
MERGE INTO SONG d USING (SELECT 9001 id FROM DUAL) x ON (d.song_id=x.id) WHEN NOT MATCHED THEN INSERT(song_id,title,album_title,genre,duration_seconds) VALUES(9001,'녹턴 2번, Op. 9 No. 2','Nocturnes, Op. 9','클래식',276);
MERGE INTO SONG_ARTIST d USING (SELECT 9001 s,9001 a FROM DUAL) x ON (d.song_id=x.s AND d.artist_id=x.a) WHEN NOT MATCHED THEN INSERT(song_id,artist_id) VALUES(9001,9001);
MERGE INTO SONG_MEDIA d USING (SELECT 9001 id FROM DUAL) x ON (d.song_id=x.id) WHEN NOT MATCHED THEN INSERT(song_id,resource_path,performer,source_url,license_name) VALUES(9001,'audio/nocturne.mp3','Musopen · The Complete Chopin Collection','https://archive.org/details/musopen-chopin','CC0 1.0');
MERGE INTO ARTIST d USING (SELECT 9001 id FROM DUAL) x ON (d.artist_id=x.id) WHEN NOT MATCHED THEN INSERT(artist_id,artist_name,country) VALUES(9001,'Frédéric Chopin','폴란드');
MERGE INTO SONG d USING (SELECT 9002 id FROM DUAL) x ON (d.song_id=x.id) WHEN NOT MATCHED THEN INSERT(song_id,title,album_title,genre,duration_seconds) VALUES(9002,'환상 즉흥곡, Op. 66','Fantaisie-Impromptu, Op. 66','클래식',326);
MERGE INTO SONG_ARTIST d USING (SELECT 9002 s,9001 a FROM DUAL) x ON (d.song_id=x.s AND d.artist_id=x.a) WHEN NOT MATCHED THEN INSERT(song_id,artist_id) VALUES(9002,9001);
MERGE INTO SONG_MEDIA d USING (SELECT 9002 id FROM DUAL) x ON (d.song_id=x.id) WHEN NOT MATCHED THEN INSERT(song_id,resource_path,performer,source_url,license_name) VALUES(9002,'audio/fantasie.mp3','Musopen · The Complete Chopin Collection','https://archive.org/details/musopen-chopin','CC0 1.0');
MERGE INTO ARTIST d USING (SELECT 9001 id FROM DUAL) x ON (d.artist_id=x.id) WHEN NOT MATCHED THEN INSERT(artist_id,artist_name,country) VALUES(9001,'Frédéric Chopin','폴란드');
MERGE INTO SONG d USING (SELECT 9003 id FROM DUAL) x ON (d.song_id=x.id) WHEN NOT MATCHED THEN INSERT(song_id,title,album_title,genre,duration_seconds) VALUES(9003,'강아지 왈츠, Op. 64 No. 1','Waltzes, Op. 64','클래식',107);
MERGE INTO SONG_ARTIST d USING (SELECT 9003 s,9001 a FROM DUAL) x ON (d.song_id=x.s AND d.artist_id=x.a) WHEN NOT MATCHED THEN INSERT(song_id,artist_id) VALUES(9003,9001);
MERGE INTO SONG_MEDIA d USING (SELECT 9003 id FROM DUAL) x ON (d.song_id=x.id) WHEN NOT MATCHED THEN INSERT(song_id,resource_path,performer,source_url,license_name) VALUES(9003,'audio/minute-waltz.mp3','Musopen · The Complete Chopin Collection','https://archive.org/details/musopen-chopin','CC0 1.0');
MERGE INTO ARTIST d USING (SELECT 9001 id FROM DUAL) x ON (d.artist_id=x.id) WHEN NOT MATCHED THEN INSERT(artist_id,artist_name,country) VALUES(9001,'Frédéric Chopin','폴란드');
MERGE INTO SONG d USING (SELECT 9004 id FROM DUAL) x ON (d.song_id=x.id) WHEN NOT MATCHED THEN INSERT(song_id,title,album_title,genre,duration_seconds) VALUES(9004,'화려한 대왈츠, Op. 18','Grande valse brillante, Op. 18','클래식',319);
MERGE INTO SONG_ARTIST d USING (SELECT 9004 s,9001 a FROM DUAL) x ON (d.song_id=x.s AND d.artist_id=x.a) WHEN NOT MATCHED THEN INSERT(song_id,artist_id) VALUES(9004,9001);
MERGE INTO SONG_MEDIA d USING (SELECT 9004 id FROM DUAL) x ON (d.song_id=x.id) WHEN NOT MATCHED THEN INSERT(song_id,resource_path,performer,source_url,license_name) VALUES(9004,'audio/grande-valse.mp3','Musopen · The Complete Chopin Collection','https://archive.org/details/musopen-chopin','CC0 1.0');

UPDATE SONG SET duration_seconds=277 WHERE song_id=9001 AND EXISTS(SELECT 1 FROM SONG_MEDIA m WHERE m.song_id=SONG.song_id AND m.resource_path='audio/nocturne.mp3');
UPDATE SONG SET duration_seconds=327 WHERE song_id=9002 AND EXISTS(SELECT 1 FROM SONG_MEDIA m WHERE m.song_id=SONG.song_id AND m.resource_path='audio/fantasie.mp3');
UPDATE SONG SET duration_seconds=108 WHERE song_id=9003 AND EXISTS(SELECT 1 FROM SONG_MEDIA m WHERE m.song_id=SONG.song_id AND m.resource_path='audio/minute-waltz.mp3');
UPDATE SONG SET duration_seconds=320 WHERE song_id=9004 AND EXISTS(SELECT 1 FROM SONG_MEDIA m WHERE m.song_id=SONG.song_id AND m.resource_path='audio/grande-valse.mp3');
COMMIT;
