ALTER TABLE users
DROP CONSTRAINT users_avatar_check,
DROP COLUMN avatar,
DROP COLUMN avatar_content_type,
ADD CONSTRAINT users_avatar_object_key_not_blank_check CHECK (btrim(avatar_object_key) <> '');
