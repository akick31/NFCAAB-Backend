ALTER TABLE players
    ADD COLUMN active TINYINT(1) NOT NULL DEFAULT 1;

ALTER TABLE players
    ADD COLUMN college_year_new VARCHAR(20) NULL;

UPDATE players
SET college_year_new = CASE college_year
    WHEN 1 THEN 'FRESHMAN'
    WHEN 2 THEN 'SOPHOMORE'
    WHEN 3 THEN 'JUNIOR'
    WHEN 4 THEN 'SENIOR'
    ELSE NULL
END;

ALTER TABLE players
    DROP COLUMN college_year;

ALTER TABLE players
    CHANGE COLUMN college_year_new college_year VARCHAR(20) NULL;
