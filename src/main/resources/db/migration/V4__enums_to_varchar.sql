ALTER TABLE releases
ALTER COLUMN release_type TYPE varchar(20) USING release_type::text;

ALTER TABLE releases
ADD CONSTRAINT releases_release_type_check CHECK (release_type IN ('ALBUM', 'SINGLE', 'EP', 'COMPILATION'));

DROP TYPE release_type_enum;
DROP TYPE user_role_enum;
