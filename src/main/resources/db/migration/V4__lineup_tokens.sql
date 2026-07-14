CREATE TABLE lineup_tokens (
    id INT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(36) NOT NULL,
    game_id INT NOT NULL,
    team VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL,
    expires_at DATETIME NOT NULL,
    used TINYINT(1) NOT NULL DEFAULT 0,
    CONSTRAINT uq_lineup_tokens_token UNIQUE (token)
);
