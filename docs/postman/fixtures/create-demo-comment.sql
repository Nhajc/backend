-- Chay thu cong trong Navicat tren database test.
-- Thay USER_ID bang id tai khoan tu response Login,
-- SONG_ID bang id bai hat tu response Upload song.
-- Lay id duoc tra ve de dien commentId trong Postman.
INSERT INTO comments (id, user_id, song_id, content)
VALUES (
    gen_random_uuid(),
    'USER_ID'::uuid,
    'SONG_ID'::uuid,
    'Bình luận demo để kiểm thử xóa'
)
RETURNING id;
