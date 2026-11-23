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

-- [01] MEMBER (회원) - 부모 테이블
INSERT INTO MEMBER
VALUES ('user01', 'DEMO_HASH_NOT_FOR_AUTH_01', '하늘', 'user01@example.com');

INSERT INTO MEMBER
VALUES ('user02', 'DEMO_HASH_NOT_FOR_AUTH_02', '서윤', 'user02@example.com');

INSERT INTO MEMBER
VALUES ('user03', 'DEMO_HASH_NOT_FOR_AUTH_03', '도현', 'user03@example.com');

-- [02] ARTIST (아티스트) - 부모 테이블
INSERT INTO ARTIST
VALUES (1, '루나', '대한민국');

INSERT INTO ARTIST
VALUES (2, '네온', '대한민국');

INSERT INTO ARTIST
VALUES (3, '하모니', '대한민국');

INSERT INTO ARTIST
VALUES (4, '에코', '대한민국');

-- [03] SONG (곡) - 부모 테이블
INSERT INTO SONG 
VALUES (101, '별빛 산책', '밤의 기록', '발라드', DATE '2025-03-12', 210);

INSERT INTO SONG 
VALUES (102, '여름의 끝', '계절의 조각', '팝', DATE '2025-07-18', 195);

INSERT INTO SONG 
VALUES (103, '푸른 파도', '바다의 노래', '인디', DATE '2024-05-21', 225);

INSERT INTO SONG 
VALUES (104, '새벽 도로', '도시의 빛', '일렉트로닉', DATE '2026-01-07', 180);

INSERT INTO SONG 
VALUES (105, '첫 페이지', '시작', '발라드', DATE '2025-11-02', 240);

-- [04] PLAYLIST (플레이리스트) - MEMBER 참조
-- 공개여부: Y = 공개, N = 비공개 (기본값 N)
INSERT INTO PLAYLIST (playlist_id, title, description, is_public, member_id)
VALUES (1001, '산책할 때 듣는 노래', '가볍게 걸을 때 듣기 좋은 곡', 'Y', 'user01');

INSERT INTO PLAYLIST (playlist_id, title, description, is_public, member_id)
VALUES (1002, '공부 집중', '공부할 때 듣는 플레이리스트', 'Y', 'user02');

INSERT INTO PLAYLIST (playlist_id, title, description, is_public, member_id)
VALUES (1003, '나만의 플레이리스트', '개인적으로 보관하는 곡', 'N', 'user03');

-- [05] SONG_ARTIST (곡-아티스트) - SONG, ARTIST 참조
-- 곡 101에는 아티스트가 2명 참여하는 N:M 관계 테스트
INSERT INTO SONG_ARTIST VALUES (101, 1);
INSERT INTO SONG_ARTIST VALUES (101, 2);
INSERT INTO SONG_ARTIST VALUES (102, 2);
INSERT INTO SONG_ARTIST VALUES (103, 3);
INSERT INTO SONG_ARTIST VALUES (104, 4);
INSERT INTO SONG_ARTIST VALUES (105, 1);

-- [06] PLAYLIST_SONG (플레이리스트-곡) - PLAYLIST, SONG 참조
-- 한 플레이리스트에서 play_order가 중복되지 않도록 지정
-- 같은 노래(101)를 서로 다른 플레이리스트에 넣어 N:M 관계 테스트
INSERT INTO PLAYLIST_SONG (playlist_id, song_id, play_order)
VALUES (1001, 101, 1);

INSERT INTO PLAYLIST_SONG (playlist_id, song_id, play_order)
VALUES (1001, 103, 2);

INSERT INTO PLAYLIST_SONG (playlist_id, song_id, play_order)
VALUES (1001, 102, 3);

INSERT INTO PLAYLIST_SONG (playlist_id, song_id, play_order)
VALUES (1002, 104, 1);

INSERT INTO PLAYLIST_SONG (playlist_id, song_id, play_order)
VALUES (1002, 101, 2);

INSERT INTO PLAYLIST_SONG (playlist_id, song_id, play_order)
VALUES (1003, 105, 1);

-- [07] PLAYLIST_LIKE (좋아요) - MEMBER, PLAYLIST 참조
-- 예시에서는 공개 플레이리스트(1001, 1002)에만 좋아요 추가
-- 주의: 공개 여부를 DB에서 강제하는 기능은 현재 CREATE TABLE만으로 구현되지 않음
INSERT INTO PLAYLIST_LIKE (member_id, playlist_id)
VALUES ('user02', 1001);

INSERT INTO PLAYLIST_LIKE (member_id, playlist_id)
VALUES ('user03', 1001);

INSERT INTO PLAYLIST_LIKE (member_id, playlist_id)
VALUES ('user01', 1002);


-- 데이터 값 확인용
SELECT 'MEMBER' AS table_name, COUNT(*) AS row_count FROM MEMBER
UNION ALL SELECT 'ARTIST', COUNT(*) FROM ARTIST
UNION ALL SELECT 'SONG', COUNT(*) FROM SONG
UNION ALL SELECT 'PLAYLIST', COUNT(*) FROM PLAYLIST
UNION ALL SELECT 'SONG_ARTIST', COUNT(*) FROM SONG_ARTIST
UNION ALL SELECT 'PLAYLIST_SONG', COUNT(*) FROM PLAYLIST_SONG
UNION ALL SELECT 'PLAYLIST_LIKE', COUNT(*) FROM PLAYLIST_LIKE;


SELECT 'MEMBER' AS table_name, COUNT(*) AS row_count FROM MEMBER
UNION ALL
SELECT 'ARTIST', COUNT(*) FROM ARTIST
UNION ALL
SELECT 'SONG', COUNT(*) FROM SONG
UNION ALL
SELECT 'PLAYLIST', COUNT(*) FROM PLAYLIST
UNION ALL
SELECT 'SONG_ARTIST', COUNT(*) FROM SONG_ARTIST
UNION ALL
SELECT 'PLAYLIST_SONG', COUNT(*) FROM PLAYLIST_SONG
UNION ALL
SELECT 'PLAYLIST_LIKE', COUNT(*) FROM PLAYLIST_LIKE;
