-- Melon TOP100 metadata extension. Original rows and keys are preserved.
SET DEFINE OFF;
DECLARE
  n NUMBER;
BEGIN
  SELECT COUNT(*) INTO n FROM user_tab_columns WHERE table_name='SONG' AND column_name='DURATION_SECONDS' AND nullable='N';
  IF n=1 THEN EXECUTE IMMEDIATE 'ALTER TABLE SONG MODIFY(duration_seconds NULL)'; END IF;
  SELECT COUNT(*) INTO n FROM user_tables WHERE table_name='MELON_CHART';
  IF n=0 THEN EXECUTE IMMEDIATE 'CREATE TABLE MELON_CHART (
    chart_at TIMESTAMP(0) CONSTRAINT pk_melon_chart PRIMARY KEY,
    fetched_at TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
    source_url VARCHAR2(500 CHAR) NOT NULL,
    row_count NUMBER(3) NOT NULL CHECK(row_count=100))'; END IF;
  SELECT COUNT(*) INTO n FROM user_tables WHERE table_name='MELON_CHART_ENTRY';
  IF n=0 THEN EXECUTE IMMEDIATE 'CREATE TABLE MELON_CHART_ENTRY (
    chart_at TIMESTAMP(0) NOT NULL,
    chart_rank NUMBER(3) NOT NULL CHECK(chart_rank BETWEEN 1 AND 100),
    song_id NUMBER(10) NOT NULL,
    melon_song_id NUMBER(10) NOT NULL,
    cover_url VARCHAR2(1000 CHAR),
    CONSTRAINT pk_melon_chart_entry PRIMARY KEY(chart_at,chart_rank),
    CONSTRAINT fk_chart_entry_chart FOREIGN KEY(chart_at) REFERENCES MELON_CHART(chart_at),
    CONSTRAINT fk_chart_entry_song FOREIGN KEY(song_id) REFERENCES SONG(song_id))'; END IF;
END;
/
COMMIT;
