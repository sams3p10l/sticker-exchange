ALTER TABLE schema_marker RENAME COLUMN opis TO description;

UPDATE schema_marker
SET description = 'skeleton'
WHERE id = 1;

CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    username VARCHAR(30) NOT NULL,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    city VARCHAR(80) NOT NULL,
    country VARCHAR(2) NOT NULL,
    about TEXT,
    joined_at TIMESTAMPTZ NOT NULL,
    by_mail BOOLEAN NOT NULL,
    in_person BOOLEAN NOT NULL,
    email_notifications BOOLEAN NOT NULL DEFAULT FALSE,
    last_seen_at TIMESTAMPTZ,
    inactivity_days INTEGER NOT NULL DEFAULT 7,
    CONSTRAINT users_inactivity_days_non_negative CHECK (inactivity_days >= 0),
    CONSTRAINT users_about_length CHECK (about IS NULL OR char_length(about) <= 2000)
);

CREATE UNIQUE INDEX users_email_lower_idx ON users (lower(email));
CREATE UNIQUE INDEX users_username_lower_idx ON users (lower(username));
