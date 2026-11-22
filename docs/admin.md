# Admin backend

Đã triển khai 4 API tạo nội dung theo api.md, API đọc thể loại, phát audio và xóa bình luận.

| API | Quyền | Request | Thành công |
|---|---|---|---|
| POST `/api/v1/admin/genres` | ADMIN | JSON `{name}` | 201 `{id}` |
| POST `/api/v1/admin/artists` | ADMIN | multipart `name` (Text), `avatar` (File, tùy chọn) | 201 `{id}` |
| POST `/api/v1/admin/releases` | ADMIN | multipart `title`, `releaseType`, `releaseDate`, `artistIds`, `genreIds` (Text), `cover` (File, tùy chọn) | 201 `{id}` |
| POST `/api/v1/admin/releases/{id}/songs` | ADMIN | multipart `trackNumber`, `title`, `file` | 201 `{id}` |
| GET `/api/v1/genres?page=0&size=20` | Đã đăng nhập | Không có body | 200 `{content: [{id,name,slug}], page, size, hasNext}` |
| GET `/api/v1/songs/{id}/audio/{quality}` | Đã đăng nhập | `low`, `medium`, `high`, `lossless` | 302 `Location` URL ký sẵn |
| DELETE `/api/v1/comments/{id}` | Tác giả hoặc ADMIN | Không có | 204, xóa kèm replies |

## Tài khoản Admin

Register luôn tạo USER; không nhận role từ client. Không có endpoint tự nâng quyền.

Để cấp ADMIN cho một tài khoản phát triển, đăng ký tài khoản đó trước rồi chạy SQL có điều kiện trong Navicat:

```sql
UPDATE users SET role = 'ADMIN'
WHERE username = 'ten_tai_khoan_can_cap_quyen';
```

Kiểm tra đúng tài khoản trước khi chạy. Sau đó Login lại; access token cũ vẫn chứa role USER. Token ADMIN cần được gửi trong header `Authorization: Bearer ...`. Thiếu token trả 401; USER gọi `/admin/**` trả 403.

## Chạy trong IntelliJ hoặc Docker

Các API tạo thể loại/nghệ sĩ/release cần PostgreSQL và MinIO như Auth. Upload audio cần thêm hai chương trình **flac** và **fdkaac**.

- Dockerfile cài flac/fdkaac và build thư viện FDK AAC đầy đủ từ tag v2.0.3 để có HE-AAC/SBR. Stage `runtime-tools` có thể build riêng để kiểm tra encoder; bước này cần mạng và mất thêm thời gian ở lần build đầu.
- Khi chạy IntelliJ trên Windows, cài hai công cụ, thêm vào PATH hoặc điền đường dẫn tuyệt đối đến executable trong `.env`:

```dotenv
AUDIO_FLAC_COMMAND=C:/tools/flac/flac.exe
AUDIO_FDKAAC_COMMAND=C:/tools/fdkaac/fdkaac.exe
AUDIO_COMMAND_TIMEOUT_SECONDS=180
```

Đây là đường dẫn ví dụ; thay bằng vị trí cài thật. Không đặt thêm dấu nháy bên trong giá trị executable. Restart Main sau khi đổi môi trường. Thiếu công cụ trả 503 với hướng dẫn cấu hình. Thời gian tối đa mỗi lệnh là 180 giây mặc định, cấu hình được từ 1–600 giây.

## Quy tắc dữ liệu

Mọi GET list đều nhận Pageable và trả `{content,page,size,hasNext}`; xem [Catalog/Library](catalog-library.md). Với `/genres`: nhận `page` bắt đầu từ 0, `size` mặc định 20 và tối đa 100. `page < 0`, `size` ngoài 1–100 hoặc offset vượt giới hạn của JPA trả 400. Repository JPA nhận `Pageable` và trả `Slice`, dùng JPQL sắp theo tên không phân biệt hoa thường rồi ID. Hibernate phân trang tại database; không tải toàn bộ danh sách vào RAM. Trang vượt dữ liệu trả content rỗng và hasNext=false. Response đã đổi từ mảng trần sang PageResponse; frontend cần đọc `response.content`.

- Genre: tên không trắng, tối đa 100 ký tự, trùng tên không phân biệt hoa thường trả 409. Slug sinh từ tên, nối UUID để tránh hai tên khác nhau tạo cùng slug.
- Artist: tên không trắng, tối đa 255 ký tự. Avatar tùy chọn.
- Release: title không trắng, tối đa 255 ký tự; loại ALBUM/SINGLE/EP/COMPILATION; ngày bắt buộc; 1–20 artist, 0–20 genre. Thiếu ID tham chiếu trả 404; ID trùng trong danh sách trả 400. Không có thể loại thì bỏ part `genreIds`. Ngày định dạng `yyyy-MM-dd`. Nhiều ID thì thêm nhiều dòng Text cùng tên `artistIds`/`genreIds`, mỗi dòng một UUID; không gửi mảng JSON.
- Tất cả API upload ảnh dùng `multipart/form-data`: avatar người dùng/nghệ sĩ có key `avatar`, cover release có key `cover`, kiểu File. Metadata gửi cùng request ở các dòng Text. Ảnh tùy chọn thì bỏ part ảnh; không gửi đường dẫn file hoặc chuỗi null. Không tự đặt header Content-Type để Postman tạo boundary. Giới hạn đầu vào 5 MiB, ảnh tối đa 40 triệu pixel; server thu nhỏ tối đa 600 px cạnh dài, giữ tỉ lệ. File nằm trong MinIO, database chỉ giữ object key.
- Các tên/title được bỏ khoảng trắng hai đầu trước validation.

## Luồng upload bài hát

1. Client tạo release, lấy `id`.
2. Gửi multipart cho từng bài với `trackNumber` ≥ 1, title ≤ 255 ký tự và part File tên `file`. Không gửi Data URI cho audio.
3. Server kiểm tra metadata FLAC thực tế: native FLAC 16-bit, sample rate ≤ 48 kHz, mono/stereo, không rỗng, thời lượng ≤ 7200 giây, dung lượng ≤ 200 MiB.
4. Decoder kiểm tra tính hợp lệ của dữ liệu audio. Encoder tạo HE-AAC 64 kbps và AAC-LC 128/256 kbps. Để hỗ trợ đầy đủ profile/bitrate, nguồn mono hoặc sample rate khác 44.1/48 kHz được resample thành PCM 48 kHz stereo cho các bản AAC. FLAC gốc được giữ nguyên. Bước giải mã/resample dùng file tạm trên đĩa, không nạp toàn bài vào RAM.
5. Upload đủ bốn file lên MinIO, sau đó ghi bài hát, quan hệ nghệ sĩ và bốn object key trong một transaction. Bài hát kế thừa nghệ sĩ của release.
6. Nếu release chưa có cover, ảnh nhúng JPEG/PNG hợp lệ trong FLAC sẽ được dùng làm cover. Ưu tiên ảnh front cover; bỏ qua ảnh nhúng không hỗ trợ hoặc quá giới hạn.
7. Chỉ trả 201 `{id}` sau khi hoàn tất. Nếu thất bại chưa có bài hát được công bố, client có thể thử lại. Trùng track trong cùng release trả 409, kể cả hai request đồng thời.

Giới hạn hai tác vụ xử lý đồng thời **trên mỗi server instance**; tác vụ thứ ba trả 429. Các lệnh có timeout và giới hạn file đầu ra. Client nên giữ kết nối đủ lâu để chờ mã hóa; timeout client không có nghĩa transaction chắc chắn chưa hoàn tất. Nếu thử lại nhận 409, kiểm tra track đã có.

Mã lỗi upload: 400 FLAC/metadata/request sai; 404 release không có; 409 trùng track; 413 quá dung lượng; 422 không thể mã hóa với profile AAC yêu cầu; 429 hết slot; 503 storage/công cụ không sẵn sàng hoặc timeout.

Các file nằm dưới `audio/songs/{songId}/{quality}.m4a` hoặc `lossless.flac`. API phát trả URL ký sẵn qua redirect 302; MinIO phục vụ Range để tua nhạc. Bài hát cũ chưa có đủ bốn file audio sẽ trả 404 ở API phát.

## Database và dọn file

Flyway V8 thêm `artists.avatar_object_key`, `releases.cover_object_key`, bảng `song_audio` và index tên genre không phân biệt hoa thường. Nếu database cũ có các genre chỉ khác hoa/thường, cần giải quyết dữ liệu trùng trước khi migration có thể thêm index; không tự xóa dữ liệu cũ.

Toàn bộ repository runtime dùng Spring Data `JpaRepository`; truy vấn bổ sung dùng JPQL, không dùng JdbcTemplate hoặc native SQL trong mã Java; tìm kiếm/thư viện gộp dùng HQL union qua JPA EntityManager. Artist, Genre, Release, Song, SongAudio, Comment và StorageCleanupJob là entity JPA. Quan hệ release–artist/genre và song–artist/audio được ánh xạ bằng association JPA. API trả DTO riêng để không lộ các trường nội bộ hoặc lazy association của entity.

Transaction Spring dùng JpaTransactionManager. Hàng đợi cleanup dùng transaction độc lập và pessimistic lock với skip-locked qua JPA/Hibernate. Flyway SQL vẫn quản lý schema, `ddl-auto=validate`; việc chuyển repository không cần migration mới và không đổi dữ liệu đã có. JDBC URL và PostgreSQL driver là cấu hình kết nối bên dưới của JPA, không phải mã truy cập JDBC trực tiếp.

Trước mỗi upload, server ghi reservation vào `storage_object_cleanup` bằng transaction độc lập. Thành công thì xóa reservation cùng transaction ghi con trỏ. Thất bại/rollback vẫn giữ reservation để worker thử dọn sau: ảnh sau 15 phút, audio sau 2 giờ. Worker bảo vệ các object đang được users, artists, releases hoặc song_audio tham chiếu. File tạm trên máy được dọn khi request kết thúc; nếu JVM/máy dừng đột ngột, thư mục tạm của hệ điều hành có thể cần dọn ngoài ứng dụng.

## Postman

Import hai file `docs/postman/Nhajc-admin.postman_collection.json` và `Nhajc-admin.postman_environment.json`, chọn **Nhajc Admin Local**, điền `adminUsername`, `adminPassword` rồi chạy theo thứ tự. Không có mật khẩu/token thật trong file export.

Collection tự lưu `genreId`, `artistId`, `releaseId`, `songId`. Chọn FLAC trong request Upload. Nếu chưa chọn file thì bỏ qua upload và các request audio. Các tài nguyên tạo ra được giữ lại. Request xóa comment chỉ chạy nếu bạn điền `commentId` muốn xóa.

Mặc định `autoGenerateData=true` sinh tên/title mẫu. Chọn PNG/JPEG tại dòng File `avatar`/`cover` trong Body → form-data của Create artist/Create release. Chưa chọn ảnh thì tạo tài nguyên không ảnh. Có sẵn FLAC và PNG trong `docs/postman/fixtures`; xem [fake-data.md](postman/fake-data.md).

Các API đọc release/artist, search, thư viện, playlist và bình luận đã triển khai trong [Catalog/Library](catalog-library.md). Bộ Postman tương ứng là `Nhajc-catalog.postman_collection.json` và `Nhajc-catalog.postman_environment.json`.
