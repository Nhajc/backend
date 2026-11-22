# Catalog, Library, Playlist và Comment

Đã triển khai toàn bộ API #11–38 trong api.md. Controller gọi service qua interface; dữ liệu đi qua Spring Data JpaRepository hoặc JPA EntityManager. Không có JDBC trực tiếp, JdbcTemplate hoặc native query. Truy vấn gộp dùng HQL union của Hibernate qua JPA để sắp xếp và phân trang tại database; không tải mọi loại tài nguyên rồi phân trang trong RAM. Tham khảo [Hibernate HQL](https://docs.hibernate.org/orm/7.4/querylanguage/html_single/#set-operations).

## Phân trang

Mọi API trả danh sách nhận `page` (từ 0), `size` (1–100). Mặc định size=20; comments/replies size=10. Ví dụ `/genres?page=1&size=10`:

```json
{
  "content": [],
  "page": 1,
  "size": 10,
  "hasNext": false
}
```

`page` và `size` phản ánh tham số đã nhận, kể cả khi trang rỗng. Dùng `Slice` hoặc truy vấn lấy size+1 để xác định hasNext. Không trả totalElements/totalPages. Tham số âm, size ngoài 1–100, sai kiểu hoặc offset quá Integer.MAX_VALUE trả 400.

GET `/releases/{id}`, `/playlists/{id}`, `/artists/{id}` cũng nhận page/size. Trường `songs` hoặc `releases` của response là PageResponse thay cho mảng. Các field summary vẫn ở top level nhờ record và JsonUnwrapped. `songCount` là tổng số bài của resource, không phải số bài trong trang. Mảng metadata artists được giữ vì bị giới hạn tối đa 20 khi tạo release.

- Release songs: trackNumber tăng dần, ID phá hòa.
- Playlist songs: theo position; cover luôn từ bài đầu toàn playlist, không phụ thuộc trang đang xem.
- Artist releases: releaseDate giảm dần, ID phá hòa.
- Genres: tên không phân biệt hoa thường, rồi ID.
- Comments gốc: mới nhất trước; replies: cũ nhất trước; ID phá hòa.

## Tìm kiếm và thư viện

Search cần q sau khi trim dài 1–100 ký tự; title/name khớp chính xác đứng trước prefix, rồi substring. Không phân biệt hoa thường; dấu `%`, `_` và `!` được xử lý như ký tự thường. Cùng mức khớp thì sắp theo tên, loại, ID. Chỉ tìm title/name, chưa có fuzzy search hoặc tìm theo lời bài hát.

Library chỉ gồm dữ liệu đã lưu/follow/favorite của người gọi. q tùy chọn; nếu gửi thì phải dài 1–100 ký tự. `RECENT` sắp theo thời điểm lưu, follow/favorite hoặc lần mở release/playlist đã lưu gần nhất; `NAME` theo tên; `ARTIST` theo tên nghệ sĩ đầu tiên theo alphabet (playlist dùng username chủ sở hữu). Artist list không nhận ARTIST sort. Mọi cách sắp xếp đều có tên, loại và ID phá hòa. `owned=true` chỉ áp dụng `/library/playlists`.

Response gộp có `type` SONG/RELEASE/PLAYLIST/ARTIST, các field item nằm cùng cấp, không bọc trong item hoặc summary. Library gộp chỉ có RELEASE/PLAYLIST. Các boolean giữ nguyên tên `isSaved`, `isFavorited`, `isFollowed`, `isPrivate`.

PUT save/follow/favorite và DELETE remove là idempotent. Save cần tài nguyên tồn tại; playlist riêng tư của người khác trả 404. Playlist sở hữu luôn tự vào library khi tạo; DELETE `/library/playlists/{id}` với playlist của chính mình trả 400, phải xóa playlist bằng endpoint tài nguyên.

## Playlist

POST/PUT nhận JSON `{ "title": "Playlist Demo", "isPrivate": true }`. Gửi đủ hai trường; title không trắng, tối đa 255 ký tự. Không có upload cover riêng: playlist lấy cover từ release của bài đầu theo use-cases hiện tại.

- Tối đa 200 playlist/người dùng và 1000 bài/playlist; vượt giới hạn trả 409.
- Tạo playlist trả 201 PlaylistResponse và tự lưu vào library.
- Chỉ chủ sở hữu được sửa/xóa/thêm/xóa/đổi thứ tự bài. Người khác sửa playlist public nhận 403; playlist private nhận 404, kể cả ADMIN.
- PUT thêm bài nối cuối; thêm lại không đổi vị trí, không lỗi khi playlist đã đủ 1000 bài. DELETE bài chưa có vẫn 204.
- PUT reorder nhận UUID[], phải đúng tập bài hiện có, không lặp/null; sai trả 400. Mảng rỗng hợp lệ khi playlist rỗng.
- Dùng pessimistic lock theo user rồi playlist để bảo vệ giới hạn và thao tác đồng thời. Ràng buộc position deferred trong V1 cho phép hoán đổi vị trí trong một transaction.
- Playlist của người khác chuyển private sẽ biến mất khỏi search/library và không xem được, dù trước đó đã lưu.

## Bình luận

POST comment/reply nhận JSON `{ "content": "Bài hát rất hay!" }`, trim, không trắng, tối đa 2000 ký tự. Chỉ reply vào comment gốc; reply cấp tiếp theo trả 400. Cả comment lẫn reply trả 201 CommentResponse. List comment chỉ trả comment gốc, kèm replyCount; list replies trả author và replyCount=0.

Tác giả hoặc ADMIN được xóa; người khác nhận 403. Xóa comment gốc xóa cả replies bằng FK cascade. Parent được khóa khi tạo reply để tránh ghi vào parent đang bị xóa. Resource không tồn tại trả 404; JWT của tài khoản đã bị xóa trả 401.

## Chạy và test

Flyway V9 chỉ thêm index hỗ trợ thứ tự/phân trang, không đổi hay xóa dữ liệu. Restart backend để áp dụng migration và code mới. Nếu chạy bản JAR trong Docker, build lại JAR rồi tạo/chạy lại container.

Import `postman/Nhajc-catalog.postman_collection.json` và environment cùng tên, chọn **Nhajc Catalog Local**. Điền username/password có sẵn và artistId/releaseId/songId thật từ bộ Admin. Giữ page=0,size=20 khi chạy toàn bộ theo thứ tự; có thể đổi để test riêng GET.

Collection tạo và xóa playlist/comment của đợt test, thay đổi trạng thái saved/followed/favorited của ID bạn cung cấp. Dùng tài khoản/dữ liệu test. Ảnh vẫn là URL ký sẵn; audio vẫn trả 302 Location. Không có endpoint upload ảnh riêng.
