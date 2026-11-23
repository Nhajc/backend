ALTER TABLE artists ADD COLUMN avatar_object_key text UNIQUE
    CONSTRAINT artists_avatar_object_key_not_blank_check CHECK (btrim(avatar_object_key) <> '');
ALTER TABLE releases ADD COLUMN cover_object_key text UNIQUE
    CONSTRAINT releases_cover_object_key_not_blank_check CHECK (btrim(cover_object_key) <> '');
ALTER TABLE playlists ADD COLUMN cover_object_key text UNIQUE
    CONSTRAINT playlists_cover_object_key_not_blank_check CHECK (btrim(cover_object_key) <> '');
ALTER TABLE storage_object_cleanup ADD COLUMN claimed boolean NOT NULL DEFAULT false;

CREATE UNIQUE INDEX ON genres (lower(name));
ALTER TABLE genres DROP CONSTRAINT genres_name_key;

CREATE TABLE song_audio (
    song_id uuid NOT NULL REFERENCES songs(id) ON DELETE CASCADE,
    quality varchar(20) NOT NULL
        CONSTRAINT song_audio_quality_valid_check CHECK (quality IN ('low', 'medium', 'high', 'lossless')),
    object_key text NOT NULL UNIQUE
        CONSTRAINT song_audio_object_key_not_blank_check CHECK (btrim(object_key) <> ''),
    PRIMARY KEY (song_id, quality)
);

DROP INDEX songs_title_idx;
DROP INDEX releases_title_idx;
DROP INDEX playlists_title_idx;
DROP INDEX artists_name_idx;
DROP INDEX comments_song_id_idx;
DROP INDEX comments_parent_id_idx;

CREATE INDEX ON comments (song_id, parent_id, commented_at DESC, id DESC);
CREATE INDEX ON comments (parent_id, commented_at, id);
CREATE INDEX ON user_saved_releases (user_id, last_activity_at DESC, release_id);
CREATE INDEX ON user_saved_playlists (user_id, last_activity_at DESC, playlist_id);
CREATE INDEX ON user_followed_artists (user_id, followed_at DESC, artist_id);
CREATE INDEX ON user_favorite_songs (user_id, favorited_at DESC, song_id);
CREATE INDEX ON songs USING gin (lower(title) gin_trgm_ops);
CREATE INDEX ON releases USING gin (lower(title) gin_trgm_ops);
CREATE INDEX ON playlists USING gin (lower(title) gin_trgm_ops);
CREATE INDEX ON artists USING gin (lower(name) gin_trgm_ops);
