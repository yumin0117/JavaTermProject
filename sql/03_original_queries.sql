-- 음악 플레이리스트 공유 시스템
-- 5단계 구현 (3) SELECT 및 JOIN 조회
-- Oracle SQL Developer / 기존 테이블과 데이터 변경 없음

-- [01] 회원별 플레이리스트 조회
SELECT
    m.member_id,
    m.nickname,
    p.playlist_id,
    p.title AS playlist_title,
    p.is_public,
    p.created_at
FROM MEMBER m
JOIN PLAYLIST p
    ON m.member_id = p.member_id
ORDER BY m.member_id, p.playlist_id;

-- [02] 곡별 참여 아티스트 조회
SELECT
    s.song_id,
    s.title AS song_title,
    a.artist_id,
    a.artist_name
FROM SONG s
JOIN SONG_ARTIST sa
    ON s.song_id = sa.song_id
JOIN ARTIST a
    ON sa.artist_id = a.artist_id
ORDER BY s.song_id, a.artist_id;

-- [03] 플레이리스트별 수록곡 및 재생순서 조회
SELECT
    p.playlist_id,
    p.title AS playlist_title,
    ps.play_order,
    s.song_id,
    s.title AS song_title,
    ps.added_at
FROM PLAYLIST p
JOIN PLAYLIST_SONG ps
    ON p.playlist_id = ps.playlist_id
JOIN SONG s
    ON ps.song_id = s.song_id
ORDER BY p.playlist_id, ps.play_order;

-- [04] 플레이리스트별 좋아요 수 조회 (좋아요가 0개인 목록도 표시)
SELECT
    p.playlist_id,
    p.title AS playlist_title,
    COUNT(pl.member_id) AS like_count
FROM PLAYLIST p
LEFT JOIN PLAYLIST_LIKE pl
    ON p.playlist_id = pl.playlist_id
GROUP BY p.playlist_id, p.title
ORDER BY p.playlist_id;


-- [00] 작업 전 데이터 수 확인: 3, 4, 5, 3, 6, 6, 3 (합계 30)
SELECT 'MEMBER' AS table_name, COUNT(*) AS row_count FROM MEMBER
UNION ALL SELECT 'ARTIST', COUNT(*) FROM ARTIST
UNION ALL SELECT 'SONG', COUNT(*) FROM SONG
UNION ALL SELECT 'PLAYLIST', COUNT(*) FROM PLAYLIST
UNION ALL SELECT 'SONG_ARTIST', COUNT(*) FROM SONG_ARTIST
UNION ALL SELECT 'PLAYLIST_SONG', COUNT(*) FROM PLAYLIST_SONG
UNION ALL SELECT 'PLAYLIST_LIKE', COUNT(*) FROM PLAYLIST_LIKE;

-- ============================================================================
-- [01] UPDATE : 플레이리스트 제목 변경
-- 대상: playlist_id = 1001
-- ============================================================================
SAVEPOINT sp_update_title;

UPDATE PLAYLIST
SET title = '저녁 산책 플레이리스트'
WHERE playlist_id = 1001;

-- 변경 확인: title = '저녁 산책 플레이리스트'
SELECT playlist_id, title FROM PLAYLIST WHERE playlist_id = 1001;

ROLLBACK TO SAVEPOINT sp_update_title;

-- 복원 확인: title = '산책할 때 듣는 노래'
SELECT playlist_id, title FROM PLAYLIST WHERE playlist_id = 1001;

-- ============================================================================
-- [02] UPDATE : 공개 여부 변경 (N -> Y)
-- 대상: playlist_id = 1003 (기존 비공개, 좋아요 없음)
-- ============================================================================
SAVEPOINT sp_update_public;

UPDATE PLAYLIST
SET is_public = 'Y'
WHERE playlist_id = 1003;

SELECT playlist_id, title, is_public FROM PLAYLIST WHERE playlist_id = 1003;

ROLLBACK TO SAVEPOINT sp_update_public;

-- 복원 확인: is_public = 'N'
SELECT playlist_id, title, is_public FROM PLAYLIST WHERE playlist_id = 1003;

-- ============================================================================
-- [03] UPDATE : 플레이리스트 내 재생순서 변경
-- 대상: playlist_id = 1001, song_id = 102, 기존 3번 -> 4번
-- 4번은 현재 비어 있으므로 UNIQUE(playlist_id, play_order)에 위배되지 않음
-- ============================================================================
SAVEPOINT sp_update_order;

UPDATE PLAYLIST_SONG
SET play_order = 4
WHERE playlist_id = 1001 AND song_id = 102;

SELECT playlist_id, song_id, play_order
FROM PLAYLIST_SONG
WHERE playlist_id = 1001
ORDER BY play_order;

ROLLBACK TO SAVEPOINT sp_update_order;

-- 복원 확인: 곡 102의 play_order가 다시 3번
SELECT playlist_id, song_id, play_order
FROM PLAYLIST_SONG
WHERE playlist_id = 1001
ORDER BY play_order;

-- ============================================================================
-- [04] DELETE : 좋아요 취소
-- 대상: user03이 playlist_id 1001에 누른 좋아요
-- ============================================================================
SAVEPOINT sp_delete_like;

DELETE FROM PLAYLIST_LIKE
WHERE member_id = 'user03' AND playlist_id = 1001;

-- 삭제 확인: 0건
SELECT COUNT(*) AS remaining_like_count
FROM PLAYLIST_LIKE
WHERE member_id = 'user03' AND playlist_id = 1001;

ROLLBACK TO SAVEPOINT sp_delete_like;

-- 복원 확인: 1건
SELECT COUNT(*) AS restored_like_count
FROM PLAYLIST_LIKE
WHERE member_id = 'user03' AND playlist_id = 1001;

-- ============================================================================
-- [05] DELETE : 플레이리스트에서 곡 제거
-- 대상: playlist_id = 1001, song_id = 103
-- SONG 원본은 지워지지 않아야 함
-- ============================================================================
SAVEPOINT sp_delete_playlist_song;

DELETE FROM PLAYLIST_SONG
WHERE playlist_id = 1001 AND song_id = 103;

-- 연결 행 삭제 확인: 0건
SELECT COUNT(*) AS playlist_song_count
FROM PLAYLIST_SONG
WHERE playlist_id = 1001 AND song_id = 103;

-- SONG 테이블 원본 유지 확인: 1건
SELECT COUNT(*) AS song_master_count
FROM SONG
WHERE song_id = 103;

ROLLBACK TO SAVEPOINT sp_delete_playlist_song;

-- 연결 행 복원 확인: 1건
SELECT COUNT(*) AS restored_playlist_song_count
FROM PLAYLIST_SONG
WHERE playlist_id = 1001 AND song_id = 103;

-- ============================================================================
-- [06] DELETE : 플레이리스트 삭제 + 자식 테이블 CASCADE 확인
-- 대상: playlist_id = 1003
-- 해당 플레이리스트에 PLAYLIST_SONG 행 1건이 있으므로 함께 삭제되는지 확인
-- MEMBER(user03), SONG(105) 원본은 유지되어야 함
-- ============================================================================
SAVEPOINT sp_delete_playlist;

DELETE FROM PLAYLIST
WHERE playlist_id = 1003;

SELECT COUNT(*) AS deleted_playlist_count
FROM PLAYLIST WHERE playlist_id = 1003;

SELECT COUNT(*) AS cascaded_song_rows
FROM PLAYLIST_SONG WHERE playlist_id = 1003;

SELECT COUNT(*) AS preserved_member_count
FROM MEMBER WHERE member_id = 'user03';

SELECT COUNT(*) AS preserved_song_count
FROM SONG WHERE song_id = 105;

ROLLBACK TO SAVEPOINT sp_delete_playlist;

-- 복원 확인: 플레이리스트 1건, 연결된 곡 1건
SELECT COUNT(*) AS restored_playlist_count
FROM PLAYLIST WHERE playlist_id = 1003;

SELECT COUNT(*) AS restored_song_rows
FROM PLAYLIST_SONG WHERE playlist_id = 1003;

-- ============================================================================
-- [07] 최종 데이터 수 확인: 작업 전과 동일한 총 30건이어야 함
-- ============================================================================
SELECT 'MEMBER' AS table_name, COUNT(*) AS row_count FROM MEMBER
UNION ALL SELECT 'ARTIST', COUNT(*) FROM ARTIST
UNION ALL SELECT 'SONG', COUNT(*) FROM SONG
UNION ALL SELECT 'PLAYLIST', COUNT(*) FROM PLAYLIST
UNION ALL SELECT 'SONG_ARTIST', COUNT(*) FROM SONG_ARTIST
UNION ALL SELECT 'PLAYLIST_SONG', COUNT(*) FROM PLAYLIST_SONG
UNION ALL SELECT 'PLAYLIST_LIKE', COUNT(*) FROM PLAYLIST_LIKE;