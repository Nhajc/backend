-- Keep track numbers unique while allowing an atomic permutation of a release.
ALTER TABLE songs DROP CONSTRAINT songs_release_id_track_number_key;
ALTER TABLE songs ADD CONSTRAINT songs_release_id_track_number_key
    UNIQUE (release_id, track_number) DEFERRABLE INITIALLY IMMEDIATE;
