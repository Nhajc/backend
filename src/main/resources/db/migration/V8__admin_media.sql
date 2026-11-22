ALTER TABLE artists ADD COLUMN avatar_object_key text
    CHECK (btrim(avatar_object_key) <> '');
ALTER TABLE releases ADD COLUMN cover_object_key text
    CHECK (btrim(cover_object_key) <> '');

CREATE UNIQUE INDEX artists_avatar_object_key_uq ON artists (avatar_object_key);
CREATE UNIQUE INDEX releases_cover_object_key_uq ON releases (cover_object_key);
CREATE UNIQUE INDEX genres_name_lower_uq ON genres (lower(name));

CREATE TABLE song_audio (
    song_id uuid NOT NULL REFERENCES songs(id) ON DELETE CASCADE,
    quality varchar(10) NOT NULL CHECK (quality IN ('low', 'medium', 'high', 'lossless')),
    object_key text NOT NULL UNIQUE CHECK (btrim(object_key) <> ''),
    PRIMARY KEY (song_id, quality)
);
