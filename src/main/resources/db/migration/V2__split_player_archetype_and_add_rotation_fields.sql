ALTER TABLE players
    ADD COLUMN batter_archetype VARCHAR(20) NULL,
    ADD COLUMN pitcher_archetype VARCHAR(20) NULL,
    ADD COLUMN pitcher_role VARCHAR(20) NULL,
    ADD COLUMN last_start_game_id INT NULL;
