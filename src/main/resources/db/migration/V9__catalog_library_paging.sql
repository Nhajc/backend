-- Supports stable ordering and bounded reads; existing data and constraints are preserved.
CREATE INDEX IF NOT EXISTS songs_release_track_idx ON songs (release_id, track_number, id);
CREATE INDEX IF NOT EXISTS comments_song_page_idx ON comments (song_id, commented_at DESC, id) WHERE parent_id IS NULL;
CREATE INDEX IF NOT EXISTS comments_reply_page_idx ON comments (parent_id, commented_at, id);
CREATE INDEX IF NOT EXISTS saved_releases_recent_idx ON user_saved_releases (user_id, last_activity_at DESC, release_id);
CREATE INDEX IF NOT EXISTS saved_playlists_recent_idx ON user_saved_playlists (user_id, last_activity_at DESC, playlist_id);
CREATE INDEX IF NOT EXISTS followed_artists_recent_idx ON user_followed_artists (user_id, followed_at DESC, artist_id);
CREATE INDEX IF NOT EXISTS favorite_songs_recent_idx ON user_favorite_songs (user_id, favorited_at DESC, song_id);
CREATE INDEX IF NOT EXISTS songs_title_lower_trgm_idx ON songs USING gin (lower(title) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS releases_title_lower_trgm_idx ON releases USING gin (lower(title) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS playlists_title_lower_trgm_idx ON playlists USING gin (lower(title) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS artists_name_lower_trgm_idx ON artists USING gin (lower(name) gin_trgm_ops);

