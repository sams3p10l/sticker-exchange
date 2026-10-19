ALTER TABLE users
    ADD COLUMN role VARCHAR(16) NOT NULL DEFAULT 'user';

ALTER TABLE users
    ADD CONSTRAINT users_role CHECK (role IN ('admin', 'user'));

UPDATE users
SET role = 'admin'
WHERE lower(username) = 'admin';

ALTER TABLE albums
    ADD COLUMN image_path VARCHAR(80);
