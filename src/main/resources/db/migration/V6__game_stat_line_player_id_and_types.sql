ALTER TABLE game_stat_line_batter
    MODIFY COLUMN game_id INT NOT NULL;

ALTER TABLE game_stat_line_batter
    ADD COLUMN player_id VARCHAR(255) NULL;

ALTER TABLE game_stat_line_pitcher
    MODIFY COLUMN game_id INT NOT NULL;

ALTER TABLE game_stat_line_pitcher
    ADD COLUMN player_id VARCHAR(255) NULL;
