ALTER TABLE users
    ADD COLUMN avatar bytea,
    ADD COLUMN avatar_content_type varchar(10),
    ADD CONSTRAINT users_avatar_check CHECK (
        (avatar IS NULL AND avatar_content_type IS NULL)
        OR (
            avatar IS NOT NULL
            AND avatar_content_type IS NOT NULL
            AND avatar_content_type IN ('image/jpeg', 'image/png')
            AND octet_length(avatar) BETWEEN 1 AND 5242880
        )
    );
