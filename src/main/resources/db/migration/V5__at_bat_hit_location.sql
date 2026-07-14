ALTER TABLE at_bats
    ADD COLUMN hit_direction VARCHAR(20) NULL,
    ADD COLUMN batted_ball_type VARCHAR(20) NULL,
    ADD COLUMN fielder_position INT NULL,
    ADD COLUMN assist_sequence VARCHAR(20) NULL;
