-- Preserve previous songs and user playlist/favorite references when replacing public catalog entries.
SET DEFINE OFF;
DECLARE
  n NUMBER;
BEGIN
  SELECT COUNT(*) INTO n FROM USER_TAB_COLUMNS WHERE TABLE_NAME='MUSIC_CATALOG' AND COLUMN_NAME='IS_ACTIVE';
  IF n=0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE MUSIC_CATALOG ADD (is_active CHAR(1) DEFAULT ''Y'' NOT NULL CONSTRAINT ck_catalog_active CHECK (is_active IN (''Y'',''N'')))';
  END IF;
END;
/
