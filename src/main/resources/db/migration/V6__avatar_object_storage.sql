ALTER TABLE users
    ADD COLUMN avatar_object_key text,
    DROP CONSTRAINT users_avatar_check,
    ADD CONSTRAINT users_avatar_check CHECK (
        (avatar IS NULL AND avatar_object_key IS NULL AND avatar_content_type IS NULL)
        OR (
            avatar_content_type IS NOT NULL
            AND avatar_content_type IN ('image/jpeg', 'image/png')
            AND (
                (avatar IS NOT NULL AND avatar_object_key IS NULL
                    AND octet_length(avatar) BETWEEN 1 AND 5242880)
                OR (avatar IS NULL AND avatar_object_key IS NOT NULL
                    AND btrim(avatar_object_key) <> '')
            )
        )
    );

CREATE UNIQUE INDEX users_avatar_object_key_uq ON users (avatar_object_key);

-- Pending uploads survive a rollback or process crash and can be cleaned up later.
CREATE TABLE storage_object_cleanup (
    object_key text PRIMARY KEY,
    delete_after timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);
