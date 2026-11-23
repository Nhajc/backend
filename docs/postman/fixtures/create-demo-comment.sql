-- Run manually against a test database.
-- Replace USER_ID with the login user ID and SONG_ID with the uploaded song ID.
-- Use the returned ID as commentId in Postman.
INSERT INTO comments (id, user_id, song_id, content)
VALUES (gen_random_uuid(), 'USER_ID'::uuid, 'SONG_ID'::uuid, 'Demo comment for deletion testing')
RETURNING id;
