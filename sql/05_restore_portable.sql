-- 복원 보조 파일: 완전히 비어 있는 별도 스키마에서만 실행합니다.
-- 기존 실습 DB에는 실행하지 마세요. DROP/DELETE/TRUNCATE가 없습니다.
-- 04 파일은 SQL Developer가 내보낸 원본 그대로 보존합니다.
-- 이 파일은 원본 CREATE 정의 + 실제 내보낸 INSERT를 FK 순서로 재배치합니다.
-- SQL Developer 원본의 DDL 종결자/중복 인덱스와 스키마 고정 문제를 피합니다.
SET DEFINE OFF;
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK;

-- 음악 플레이리스트 DB 작성 (추후 JAVA 과제에도 사용예정)
-- 1. MEMBER — 회원 테이블
CREATE TABLE MEMBER (
    member_id     VARCHAR2(30 CHAR)
                  CONSTRAINT pk_member PRIMARY KEY,
    password_hash VARCHAR2(255 CHAR) NOT NULL,
    nickname      VARCHAR2(50 CHAR) NOT NULL,
    email         VARCHAR2(254 CHAR) NOT NULL
);

-- 2. ARTIST — 아티스트 테이블
CREATE TABLE ARTIST (
    artist_id   NUMBER(10)
                CONSTRAINT pk_artist PRIMARY KEY,
    artist_name VARCHAR2(120 CHAR) NOT NULL,
    country     VARCHAR2(80 CHAR) NOT NULL
);

-- 3. SONG — 곡 테이블
CREATE TABLE SONG (
    song_id          NUMBER(10)
                     CONSTRAINT pk_song PRIMARY KEY,
    title            VARCHAR2(200 CHAR) NOT NULL,
    album_title      VARCHAR2(200 CHAR),
    genre            VARCHAR2(80 CHAR),
    release_date     DATE,
    duration_seconds NUMBER(8) NOT NULL,

    CONSTRAINT ck_song_duration
        CHECK (duration_seconds > 0)
);

-- 4. PLAYLIST — 플레이리스트 테이블
CREATE TABLE PLAYLIST (
    playlist_id NUMBER(10)
                CONSTRAINT pk_playlist PRIMARY KEY,
    title       VARCHAR2(150 CHAR) NOT NULL,
    description VARCHAR2(1000 CHAR),
    created_at  TIMESTAMP(6)
                DEFAULT SYSTIMESTAMP NOT NULL,
    is_public   CHAR(1 CHAR) DEFAULT 'N' NOT NULL,
    member_id   VARCHAR2(30 CHAR) NOT NULL,

    CONSTRAINT ck_playlist_public
        CHECK (is_public IN ('Y', 'N')),

    CONSTRAINT fk_playlist_member
        FOREIGN KEY (member_id)
        REFERENCES MEMBER (member_id)
);

-- 5. SONG_ARTIST — 곡·아티스트 관계 테이블
CREATE TABLE SONG_ARTIST (
    song_id   NUMBER(10) NOT NULL,
    artist_id NUMBER(10) NOT NULL,

    CONSTRAINT pk_song_artist
        PRIMARY KEY (song_id, artist_id),

    CONSTRAINT fk_sa_song
        FOREIGN KEY (song_id)
        REFERENCES SONG (song_id),

    CONSTRAINT fk_sa_artist
        FOREIGN KEY (artist_id)
        REFERENCES ARTIST (artist_id)
);

-- 6. PLAYLIST_SONG — 플레이리스트·곡 관계 테이블
CREATE TABLE PLAYLIST_SONG (
    playlist_id NUMBER(10) NOT NULL,
    song_id     NUMBER(10) NOT NULL,
    added_at    TIMESTAMP(6)
                DEFAULT SYSTIMESTAMP NOT NULL,
    play_order  NUMBER(6) NOT NULL,

    CONSTRAINT pk_playlist_song
        PRIMARY KEY (playlist_id, song_id),

    CONSTRAINT uq_playlist_order
        UNIQUE (playlist_id, play_order),

    CONSTRAINT ck_playlist_order
        CHECK (play_order >= 1),

    CONSTRAINT fk_ps_playlist
        FOREIGN KEY (playlist_id)
        REFERENCES PLAYLIST (playlist_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_ps_song
        FOREIGN KEY (song_id)
        REFERENCES SONG (song_id)
);

-- 7. PLAYLIST_LIKE — 좋아요 테이블
CREATE TABLE PLAYLIST_LIKE (
    member_id   VARCHAR2(30 CHAR) NOT NULL,
    playlist_id NUMBER(10) NOT NULL,
    liked_at    TIMESTAMP(6)
                DEFAULT SYSTIMESTAMP NOT NULL,

    CONSTRAINT pk_playlist_like
        PRIMARY KEY (member_id, playlist_id),

    CONSTRAINT fk_pl_member
        FOREIGN KEY (member_id)
        REFERENCES MEMBER (member_id),

    CONSTRAINT fk_pl_playlist
        FOREIGN KEY (playlist_id)
        REFERENCES PLAYLIST (playlist_id)
        ON DELETE CASCADE
);

-- MEMBER
INSERT INTO MEMBER (MEMBER_ID,PASSWORD_HASH,NICKNAME,EMAIL) values ('user01','DEMO_HASH_NOT_FOR_AUTH_01','하늘','user01@example.com');
INSERT INTO MEMBER (MEMBER_ID,PASSWORD_HASH,NICKNAME,EMAIL) values ('user02','DEMO_HASH_NOT_FOR_AUTH_02','서윤','user02@example.com');
INSERT INTO MEMBER (MEMBER_ID,PASSWORD_HASH,NICKNAME,EMAIL) values ('user03','DEMO_HASH_NOT_FOR_AUTH_03','도현','user03@example.com');

-- ARTIST
INSERT INTO ARTIST (ARTIST_ID,ARTIST_NAME,COUNTRY) values (1,'루나','대한민국');
INSERT INTO ARTIST (ARTIST_ID,ARTIST_NAME,COUNTRY) values (2,'네온','대한민국');
INSERT INTO ARTIST (ARTIST_ID,ARTIST_NAME,COUNTRY) values (3,'하모니','대한민국');
INSERT INTO ARTIST (ARTIST_ID,ARTIST_NAME,COUNTRY) values (4,'에코','대한민국');

-- SONG
INSERT INTO SONG (SONG_ID,TITLE,ALBUM_TITLE,GENRE,RELEASE_DATE,DURATION_SECONDS) values (101,'별빛 산책','밤의 기록','발라드',to_date('25/03/12','RR/MM/DD'),210);
INSERT INTO SONG (SONG_ID,TITLE,ALBUM_TITLE,GENRE,RELEASE_DATE,DURATION_SECONDS) values (102,'여름의 끝','계절의 조각','팝',to_date('25/07/18','RR/MM/DD'),195);
INSERT INTO SONG (SONG_ID,TITLE,ALBUM_TITLE,GENRE,RELEASE_DATE,DURATION_SECONDS) values (103,'푸른 파도','바다의 노래','인디',to_date('24/05/21','RR/MM/DD'),225);
INSERT INTO SONG (SONG_ID,TITLE,ALBUM_TITLE,GENRE,RELEASE_DATE,DURATION_SECONDS) values (104,'새벽 도로','도시의 빛','일렉트로닉',to_date('26/01/07','RR/MM/DD'),180);
INSERT INTO SONG (SONG_ID,TITLE,ALBUM_TITLE,GENRE,RELEASE_DATE,DURATION_SECONDS) values (105,'첫 페이지','시작','발라드',to_date('25/11/02','RR/MM/DD'),240);

-- PLAYLIST
INSERT INTO PLAYLIST (PLAYLIST_ID,TITLE,DESCRIPTION,CREATED_AT,IS_PUBLIC,MEMBER_ID) values (1001,'산책할 때 듣는 노래','가볍게 걸을 때 듣기 좋은 곡',to_timestamp('26/11/12 15:32:11.132000000','RR/MM/DD HH24:MI:SSXFF'),'Y','user01');
INSERT INTO PLAYLIST (PLAYLIST_ID,TITLE,DESCRIPTION,CREATED_AT,IS_PUBLIC,MEMBER_ID) values (1002,'공부 집중','공부할 때 듣는 플레이리스트',to_timestamp('26/11/12 15:32:13.221000000','RR/MM/DD HH24:MI:SSXFF'),'Y','user02');
INSERT INTO PLAYLIST (PLAYLIST_ID,TITLE,DESCRIPTION,CREATED_AT,IS_PUBLIC,MEMBER_ID) values (1003,'나만의 플레이리스트','개인적으로 보관하는 곡',to_timestamp('26/11/12 15:32:14.509000000','RR/MM/DD HH24:MI:SSXFF'),'N','user03');

-- SONG_ARTIST
INSERT INTO SONG_ARTIST (SONG_ID,ARTIST_ID) values (101,1);
INSERT INTO SONG_ARTIST (SONG_ID,ARTIST_ID) values (101,2);
INSERT INTO SONG_ARTIST (SONG_ID,ARTIST_ID) values (102,2);
INSERT INTO SONG_ARTIST (SONG_ID,ARTIST_ID) values (103,3);
INSERT INTO SONG_ARTIST (SONG_ID,ARTIST_ID) values (104,4);
INSERT INTO SONG_ARTIST (SONG_ID,ARTIST_ID) values (105,1);

-- PLAYLIST_SONG
INSERT INTO PLAYLIST_SONG (PLAYLIST_ID,SONG_ID,ADDED_AT,PLAY_ORDER) values (1001,101,to_timestamp('26/11/12 15:32:31.274000000','RR/MM/DD HH24:MI:SSXFF'),1);
INSERT INTO PLAYLIST_SONG (PLAYLIST_ID,SONG_ID,ADDED_AT,PLAY_ORDER) values (1001,103,to_timestamp('26/11/12 15:32:33.370000000','RR/MM/DD HH24:MI:SSXFF'),2);
INSERT INTO PLAYLIST_SONG (PLAYLIST_ID,SONG_ID,ADDED_AT,PLAY_ORDER) values (1001,102,to_timestamp('26/11/12 15:32:34.522000000','RR/MM/DD HH24:MI:SSXFF'),3);
INSERT INTO PLAYLIST_SONG (PLAYLIST_ID,SONG_ID,ADDED_AT,PLAY_ORDER) values (1002,104,to_timestamp('26/11/12 15:32:35.557000000','RR/MM/DD HH24:MI:SSXFF'),1);
INSERT INTO PLAYLIST_SONG (PLAYLIST_ID,SONG_ID,ADDED_AT,PLAY_ORDER) values (1002,101,to_timestamp('26/11/12 15:32:38.811000000','RR/MM/DD HH24:MI:SSXFF'),2);
INSERT INTO PLAYLIST_SONG (PLAYLIST_ID,SONG_ID,ADDED_AT,PLAY_ORDER) values (1003,105,to_timestamp('26/11/12 15:32:40.030000000','RR/MM/DD HH24:MI:SSXFF'),1);

-- PLAYLIST_LIKE
INSERT INTO PLAYLIST_LIKE (MEMBER_ID,PLAYLIST_ID,LIKED_AT) values ('user02',1001,to_timestamp('26/11/12 15:32:44.565000000','RR/MM/DD HH24:MI:SSXFF'));
INSERT INTO PLAYLIST_LIKE (MEMBER_ID,PLAYLIST_ID,LIKED_AT) values ('user03',1001,to_timestamp('26/11/12 15:32:45.951000000','RR/MM/DD HH24:MI:SSXFF'));
INSERT INTO PLAYLIST_LIKE (MEMBER_ID,PLAYLIST_ID,LIKED_AT) values ('user01',1002,to_timestamp('26/11/12 15:32:47.011000000','RR/MM/DD HH24:MI:SSXFF'));

COMMIT;
-- 이어서 02_add_sequence.sql을 실행합니다.
