CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TYPE release_type_enum AS ENUM (
    'ALBUM',
    'SINGLE',
    'EP',
    'COMPILATION'
);

CREATE TYPE user_role_enum AS ENUM (
    'USER',
    'ADMIN'
);

CREATE TABLE users (
    id uuid PRIMARY KEY,
    username text NOT NULL UNIQUE,
    email text NOT NULL UNIQUE,
    password text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    role user_role_enum NOT NULL DEFAULT 'USER',

    CONSTRAINT users_password_not_blank_check CHECK (btrim(password) <> '')
);

CREATE TABLE refresh_tokens (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    family_id uuid NOT NULL,
    token_hash text NOT NULL UNIQUE,
    expires_at timestamptz NOT NULL,
    used_at timestamptz
);

CREATE TABLE artists (
    id uuid PRIMARY KEY,
    name text NOT NULL,

    CONSTRAINT artists_name_not_blank_check CHECK (btrim(name) <> '')
);

CREATE TABLE genres (
    id uuid PRIMARY KEY,
    name text NOT NULL UNIQUE,
    slug text NOT NULL UNIQUE,

    CONSTRAINT genres_name_not_blank_check CHECK (btrim(name) <> '')
);

CREATE TABLE releases (
    id uuid PRIMARY KEY,
    title text NOT NULL,
    release_type release_type_enum NOT NULL,
    release_date date NOT NULL,

    CONSTRAINT releases_title_not_blank_check CHECK (btrim(title) <> '')
);

CREATE TABLE release_genres (
    release_id uuid NOT NULL REFERENCES releases (id) ON DELETE CASCADE,
    genre_id uuid NOT NULL REFERENCES genres (id) ON DELETE CASCADE,

    PRIMARY KEY (release_id, genre_id)
);

CREATE TABLE release_artists (
    release_id uuid NOT NULL REFERENCES releases (id) ON DELETE CASCADE,
    artist_id uuid NOT NULL REFERENCES artists (id) ON DELETE CASCADE,

    PRIMARY KEY (release_id, artist_id)
);

CREATE TABLE songs (
    id uuid PRIMARY KEY,
    title text NOT NULL,
    duration_seconds int NOT NULL,
    track_number int NOT NULL,
    release_id uuid NOT NULL REFERENCES releases (id) ON DELETE CASCADE,

    UNIQUE (release_id, track_number),
    CONSTRAINT songs_title_not_blank_check CHECK (btrim(title) <> ''),
    CONSTRAINT songs_duration_seconds_positive_check CHECK (duration_seconds > 0),
    CONSTRAINT songs_track_number_positive_check CHECK (track_number > 0)
);

CREATE TABLE song_artists (
    song_id uuid NOT NULL REFERENCES songs (id) ON DELETE CASCADE,
    artist_id uuid NOT NULL REFERENCES artists (id) ON DELETE CASCADE,

    PRIMARY KEY (song_id, artist_id)
);

CREATE TABLE playlists (
    id uuid PRIMARY KEY,
    title text NOT NULL,
    owner_id uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    is_private boolean NOT NULL DEFAULT true,

    CONSTRAINT playlists_title_not_blank_check CHECK (btrim(title) <> '')
);

CREATE TABLE playlist_songs (
    playlist_id uuid NOT NULL REFERENCES playlists (id) ON DELETE CASCADE,
    song_id uuid NOT NULL REFERENCES songs (id) ON DELETE CASCADE,
    position int NOT NULL DEFAULT 0,

    PRIMARY KEY (playlist_id, song_id),
    UNIQUE (playlist_id, position) DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT playlist_songs_position_non_negative_check CHECK (position >= 0)
);

CREATE TABLE user_saved_releases (
    user_id uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    release_id uuid NOT NULL REFERENCES releases (id) ON DELETE CASCADE,
    last_activity_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, release_id)
);

CREATE TABLE user_saved_playlists (
    user_id uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    playlist_id uuid NOT NULL REFERENCES playlists (id) ON DELETE CASCADE,
    last_activity_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, playlist_id)
);

CREATE TABLE user_followed_artists (
    user_id uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    artist_id uuid NOT NULL REFERENCES artists (id) ON DELETE CASCADE,
    followed_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, artist_id)
);

CREATE TABLE user_favorite_songs (
    user_id uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    song_id uuid NOT NULL REFERENCES songs (id) ON DELETE CASCADE,
    favorited_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, song_id)
);

CREATE TABLE user_play_history (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    song_id uuid NOT NULL REFERENCES songs (id) ON DELETE CASCADE,
    played_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE comments (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    song_id uuid NOT NULL REFERENCES songs (id) ON DELETE CASCADE,
    parent_id uuid REFERENCES comments (id) ON DELETE CASCADE,
    content text NOT NULL,
    commented_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT comments_content_not_blank_check CHECK (btrim(content) <> '')
);

CREATE INDEX ON refresh_tokens (family_id);
CREATE INDEX ON releases (release_type);
CREATE INDEX ON release_artists (artist_id);
CREATE INDEX ON song_artists (artist_id);
CREATE INDEX ON playlists (owner_id);
CREATE INDEX ON user_saved_playlists (playlist_id);
CREATE INDEX ON user_saved_releases (release_id);
CREATE INDEX ON comments (song_id);
CREATE INDEX ON comments (user_id);
CREATE INDEX ON comments (parent_id);
CREATE INDEX ON songs USING gin (title gin_trgm_ops);
CREATE INDEX ON releases USING gin (title gin_trgm_ops);
CREATE INDEX ON playlists USING gin (title gin_trgm_ops);
CREATE INDEX ON artists USING gin (name gin_trgm_ops);
