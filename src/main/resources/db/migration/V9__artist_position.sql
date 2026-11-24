ALTER TABLE song_artists ADD COLUMN position int;
ALTER TABLE release_artists ADD COLUMN position int;

UPDATE song_artists AS target
SET position = ranked.position
FROM (
    SELECT
        sa.song_id,
        sa.artist_id,
        row_number() OVER (PARTITION BY sa.song_id ORDER BY lower(a.name), a.id) - 1 AS position
    FROM song_artists AS sa
    INNER JOIN artists AS a ON sa.artist_id = a.id
) AS ranked
WHERE target.song_id = ranked.song_id AND target.artist_id = ranked.artist_id;

UPDATE release_artists AS target
SET position = ranked.position
FROM (
    SELECT
        ra.release_id,
        ra.artist_id,
        row_number() OVER (PARTITION BY ra.release_id ORDER BY lower(a.name), a.id) - 1 AS position
    FROM release_artists AS ra
    INNER JOIN artists AS a ON ra.artist_id = a.id
) AS ranked
WHERE target.release_id = ranked.release_id AND target.artist_id = ranked.artist_id;

ALTER TABLE song_artists
ALTER COLUMN position SET NOT NULL,
ADD UNIQUE (song_id, position),
ADD CONSTRAINT song_artists_position_non_negative_check CHECK (position >= 0);

ALTER TABLE release_artists
ALTER COLUMN position SET NOT NULL,
ADD UNIQUE (release_id, position),
ADD CONSTRAINT release_artists_position_non_negative_check CHECK (position >= 0);
