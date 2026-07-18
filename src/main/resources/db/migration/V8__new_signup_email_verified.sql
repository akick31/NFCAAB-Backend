ALTER TABLE new_signup ADD COLUMN email_verified TINYINT(1) NOT NULL DEFAULT 0;

UPDATE new_signup SET email_verified = 1 WHERE approved = 1;
