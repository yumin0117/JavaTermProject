-- 기존 데이터 보존. 프로그램을 모두 종료한 상태에서 최초 한 번 실행합니다.
-- 기존 시퀀스가 있으면 변경하지 않습니다. MAX는 최초 초기화에만 사용합니다.
SET SERVEROUTPUT ON;
DECLARE
  v_exists NUMBER;
  v_start NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_exists FROM user_sequences WHERE sequence_name='SEQ_PLAYLIST';
  IF v_exists=0 THEN
    SELECT GREATEST(NVL(MAX(playlist_id),0)+1,1) INTO v_start FROM PLAYLIST;
    EXECUTE IMMEDIATE 'CREATE SEQUENCE SEQ_PLAYLIST START WITH ' || TO_CHAR(v_start,'FM99999999990') || ' INCREMENT BY 1 MAXVALUE 9999999999 NOCYCLE NOCACHE';
    DBMS_OUTPUT.PUT_LINE('SEQ_PLAYLIST 생성 완료');
  ELSE
    DBMS_OUTPUT.PUT_LINE('기존 SEQ_PLAYLIST 유지');
  END IF;
END;
/
