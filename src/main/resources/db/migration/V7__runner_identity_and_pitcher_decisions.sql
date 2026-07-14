ALTER TABLE game
    ADD COLUMN runner_on_first_pitcher INT NULL,
    ADD COLUMN runner_on_second_pitcher INT NULL,
    ADD COLUMN runner_on_third_pitcher INT NULL;

ALTER TABLE at_bats
    ADD COLUMN runner_on_first_pitcher INT NULL,
    ADD COLUMN runner_on_second_pitcher INT NULL,
    ADD COLUMN runner_on_third_pitcher INT NULL,
    ADD COLUMN runner_on_first_pitcher_after INT NULL,
    ADD COLUMN runner_on_second_pitcher_after INT NULL,
    ADD COLUMN runner_on_third_pitcher_after INT NULL;

CREATE TABLE run_event (
    id INT AUTO_INCREMENT PRIMARY KEY,
    game_id INT NOT NULL,
    at_bat_id INT NOT NULL,
    inning INT NOT NULL,
    scoring_team VARCHAR(255) NOT NULL,
    scoring_player_uniform_number INT NULL,
    charged_pitcher_uniform_number INT NULL,
    charged_pitcher_team VARCHAR(255) NOT NULL
);

ALTER TABLE game_stat_line_pitcher
    ADD COLUMN win TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN loss TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN save TINYINT(1) NOT NULL DEFAULT 0;
