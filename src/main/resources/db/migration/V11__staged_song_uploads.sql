ALTER TABLE songs ADD COLUMN description text;
CREATE TABLE staged_song_uploads (
    id uuid PRIMARY KEY,
    release_id uuid NOT NULL REFERENCES releases(id) ON DELETE CASCADE,
    actor_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    duration_seconds int NOT NULL CHECK(duration_seconds > 0),
    low_key text NOT NULL, medium_key text NOT NULL, high_key text NOT NULL, lossless_key text NOT NULL,
    cover_key text,
    expires_at timestamptz NOT NULL
);
CREATE INDEX staged_song_uploads_expiry ON staged_song_uploads(expires_at);
