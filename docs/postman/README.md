# Test Auth và Users bằng Postman

Có sẵn [dữ liệu giả và file upload mẫu](fake-data.md) cho các API hiện có. File [sample-data.json](sample-data.json) chứa body tham khảo cho từng API.

Bộ Admin riêng gồm `Nhajc-admin.postman_collection.json` và `Nhajc-admin.postman_environment.json`. Chọn **Nhajc Admin Local**, điền tài khoản ADMIN và chạy theo thứ tự; xem [hướng dẫn Admin](../admin.md). API tạo nội dung, đọc thể loại, phát audio và xóa bình luận đã được bổ sung trong bộ này.

Create artist và Create release cũng dùng **Body → form-data**. Chọn `avatar`/`cover` kiểu File; metadata kiểu Text. Nhiều artist/genre thì lặp key `artistIds`/`genreIds`, mỗi dòng một UUID. Không tự đặt Content-Type. Import lại collection Admin để thay các request JSON cũ.

Bộ **Catalog & Library**: import `Nhajc-catalog.postman_collection.json` và `Nhajc-catalog.postman_environment.json`, chọn **Nhajc Catalog Local**. Điền username/password có sẵn, artistId/releaseId/songId đã tạo từ Admin, giữ page=0,size=20 rồi chạy theo thứ tự. Bộ này tạo rồi xóa playlist/comment của đợt test, đồng thời thay đổi trạng thái lưu/follow/favorite của ID được nhập. Xem [catalog-library.md](../catalog-library.md).

GET List genres đã phân trang: `page=0&size=20`, chỉnh qua `genrePage`/`genreSize`. Response đổi thành `{content,page,size,hasNext}`; frontend và test đọc danh sách ở `content`. Tăng page khi hasNext=true; trang vượt dữ liệu trả content rỗng.

Bộ Auth/Users này có 19 request cho 6 API Auth/Users. Các API Admin nằm trong collection riêng; Catalog/Library/Playlist/Comment đã có trong bộ Catalog & Library riêng.

## Import và chạy

1. Khởi động PostgreSQL, MinIO và backend. Backend mặc định ở `http://localhost:8080`.
2. Trong Postman, chọn **Import** và chọn cả hai file cùng thư mục này:
   - `Nhajc.postman_collection.json`
   - `Nhajc-local.postman_environment.json`
3. Chọn environment **Nhajc Local**. Nếu backend chạy ở cổng khác, sửa `baseUrl`; không thêm `/api/v1` vào giá trị này.
4. Gửi lần lượt các request theo số thứ tự hoặc chọn **Run collection**, một iteration.
5. Xem kết quả kiểm tra ở **Test Results** của từng request hoặc báo cáo Runner.

Mặc định `autoGenerateUser=true`. Mỗi lần gửi **01 - Register**, script tạo username/email/password ngẫu nhiên và lưu vào environment, sau đó đăng ký tài khoản mới. Bạn không cần điền tài khoản trước. Tài khoản được giữ trong database để bạn dùng tiếp.

Chạy toàn bộ collection sẽ xóa avatar của tài khoản đang thử và logout phiên cuối. Sau đó cần Login lại để tiếp tục gọi API Users. Các lần Register/Login tạo các phiên riêng; Logout thu hồi phiên đang được lưu trong environment.

## Các nhóm request

| Nhóm | Request | Kết quả mong đợi |
|---|---|---|
| Auth | Register | 201, lưu token và user |
| Auth | Login bằng username / email | 200, thay token đang lưu |
| Auth | Refresh tokens | 200, thay cả access token và refresh token |
| Users | Get my profile | 200, thông tin tài khoản và `avatarUrl` |
| Users | Upload avatar | 200 nếu đã chọn file; bỏ qua nếu chưa chọn |
| Users | Remove avatar | 200, `avatarUrl=null` |
| Kiểm tra lỗi | Đăng ký trùng | 409 |
| Kiểm tra lỗi | Username chứa @, email sai, mật khẩu ngắn hoặc quá 72 byte UTF-8 | 400 |
| Kiểm tra lỗi | Login sai mật khẩu / tài khoản không tồn tại | 401, cùng thông báo |
| Kiểm tra lỗi | Refresh token giả, thiếu Bearer hoặc JWT sai | 401 |
| Logout | Logout | 204, xóa token đang lưu |
| Logout | Refresh sau logout | 401 |

Các request lỗi cố ý gửi dữ liệu sai. Chúng được coi là **pass** khi server trả đúng mã lỗi mong đợi. Không tự gây lỗi server để kiểm tra 500 trong collection này.

## Dùng tài khoản tự nhập hoặc tài khoản đã có

Đặt `autoGenerateUser=false`, rồi điền `username`, `email`, `password` trong environment.

- Đăng ký mới: username 3–50 ký tự, không chứa `@` hoặc khoảng trắng; email hợp lệ, tối đa 254 ký tự; mật khẩu 12–72 ký tự và tối đa 72 byte UTF-8. Chạy từ Register.
- Tài khoản đã có: bắt đầu từ **02 - Login bằng username** hoặc **03 - Login bằng email**. Không chạy Register nếu không muốn nhận 409.
- Login vẫn chấp nhận mật khẩu cũ ngắn hơn 12 ký tự.
- `accessToken`, `refreshToken`, `userId`, `avatarUrl` được script cập nhật tự động, không cần chép tay.

Refresh token chỉ dùng một lần. Request Refresh luôn lưu token mới. Nếu tự gửi lại token cũ, server thu hồi nhóm refresh token của phiên đó và yêu cầu đăng nhập lại.

## Tải avatar

Mở **06 - Upload avatar** → **Body** → **form-data** → dòng `avatar` có kiểu **File** → chọn JPEG/PNG tối đa 5 MB. Gửi request sau khi Login.

Giữ `_charset_=UTF-8`; không tự thêm header `Content-Type` vì Postman cần tạo multipart boundary. Nếu chưa chọn file, script bỏ qua request này. Đây là cách dùng `PUT /api/v1/users/me`, không có endpoint upload ảnh riêng.

Sau khi upload, response có `avatarUrl`. Sao chép toàn bộ URL vào trình duyệt để xem ảnh. URL ký sẵn mặc định có hạn 1 giờ; gọi Get my profile để lấy URL mới khi hết hạn.

**07 - Remove avatar** gửi cùng API PUT nhưng không có part `avatar`, nên server xóa ảnh hiện tại.

## Khi request không chạy như mong đợi

- Không kết nối được: kiểm tra backend đã chạy và `baseUrl` đúng cổng.
- Biến không có giá trị: kiểm tra đã chọn environment Nhajc Local.
- Users trả 401: chạy Login lại để lấy token mới.
- Upload bị bỏ qua: chọn file tại Body → form-data → avatar.
- Upload trả 503: kiểm tra MinIO và cấu hình storage của backend.
- Upload trả 400 `Invalid avatar part`: request chưa có file part `avatar` hợp lệ. Chọn lại file ở dòng kiểu File, kiểm tra quyền đọc file và request trong Postman Console. Dấu cảnh báo file ngoài working directory chủ yếu nói về khả năng chia sẻ file; cần xem nội dung lỗi trong Console để biết file có gửi được hay không.

Tham khảo [cách import của Postman](https://learning.postman.com/docs/getting-started/importing-and-exporting/importing-data/) và [cách script bỏ qua request](https://learning.postman.com/docs/tests-and-scripts/write-scripts/postman-sandbox-reference/pm-execution).
