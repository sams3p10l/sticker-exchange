CREATE TABLE publishers (
    id UUID PRIMARY KEY,
    name VARCHAR(80) NOT NULL
);

CREATE TABLE albums (
    id UUID PRIMARY KEY,
    publisher_id UUID NOT NULL REFERENCES publishers (id),
    slug VARCHAR(40) NOT NULL UNIQUE,
    title VARCHAR(120) NOT NULL
);

CREATE TABLE stickers (
    id UUID PRIMARY KEY,
    album_id UUID NOT NULL REFERENCES albums (id),
    code VARCHAR(16) NOT NULL,
    sort_order INTEGER NOT NULL,
    name VARCHAR(80),
    CONSTRAINT stickers_album_code UNIQUE (album_id, code)
);

CREATE INDEX stickers_album_order_idx ON stickers (album_id, sort_order);

CREATE TABLE collection_albums (
    user_id UUID NOT NULL REFERENCES users (id),
    album_id UUID NOT NULL REFERENCES albums (id),
    added_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, album_id)
);

CREATE INDEX collection_albums_album_idx ON collection_albums (album_id);

CREATE TABLE sticker_states (
    user_id UUID NOT NULL REFERENCES users (id),
    sticker_id UUID NOT NULL REFERENCES stickers (id),
    status VARCHAR(16) NOT NULL,
    duplicate_count INTEGER,
    PRIMARY KEY (user_id, sticker_id),
    CONSTRAINT sticker_states_status CHECK (status IN ('have', 'duplicate', 'want')),
    CONSTRAINT sticker_states_count CHECK (
        (status = 'duplicate' AND duplicate_count >= 1)
        OR (status <> 'duplicate' AND duplicate_count IS NULL)
    )
);

CREATE INDEX sticker_states_sticker_idx ON sticker_states (sticker_id);
