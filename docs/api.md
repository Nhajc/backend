# Nhajc API

- `/api/v1` · port 8080 · JSON responses; multipart requests where specified · errors: RFC 9457 `ProblemDetail`
- auth: `Bearer <accessToken>` (15 min); refresh token 30 days, rotated each use, reuse revokes its family.
- Access: **P** public · **U** user · **A** admin.
- Postman files for the implemented APIs are in `docs/postman/`. Import the collection and local environment, select `Nhajc Local`, and set `baseUrl`, `username`, `email`, and `password`. Use a new username/email for registration. Login can use the username or email in its `usernameOrEmail` field.
- Register, login, and refresh save the current tokens in the selected environment. Users requests inherit Bearer authentication from the collection. Logout clears the saved tokens.
- In `Upload avatar`, select a JPEG/PNG file in Body → form-data → `avatar` before sending; the request is skipped when no file is selected. `Remove avatar` sends `_charset_=UTF-8` with the file part omitted. Running the entire collection includes avatar removal and logout, so use a test account.
- Paging: `page=0`, `size=20` (1–100). Enums are UPPERCASE.
- JSON requests: send every field; only `|null` fields may be `null`. `PUT` replaces the whole object.
- Image uploads in JSON requests use `data:image/jpeg|png;base64,…`, ≤ 5 MB decoded, `null` = no image. Profile updates use the multipart rules below.
- Image response fields `avatar` and `cover` contain presigned HTTP(S) GET URLs, or `null` when no image exists. URLs expire after 1 hour by default (`MINIO_URL_EXPIRY_SECONDS`). Fetch the resource again to obtain a fresh URL after expiry. A URL can stop working earlier if its image is deleted or replaced. Anyone holding a valid URL can fetch the image until access expires or the object is removed.

## 1. Endpoints

### 1.1 Auth / user
| # | Method | Path | Acc | Body | Returns | Notes |
|---|---|---|---|---|---|---|
| 1 | POST | `/auth/register` | P | `RegisterRequest` | 201 `AuthResponse` | 409 username or email taken |
| 2 | POST | `/auth/login` | P | `LoginRequest` | `AuthResponse` | 401 bad credentials |
| 3 | POST | `/auth/refresh` | P | `RefreshTokenRequest` | `AuthResponse` | |
| 4 | POST | `/auth/logout` | P | `RefreshTokenRequest` | 204 | |
| 5 | GET | `/users/me` | U | | `UserResponse` | |
| 6 | PUT | `/users/me` | U | multipart `UpdateProfileRequest` | 200 `UserResponse` | Only the avatar for now; see multipart rules below |

Profile update (#6) uses `multipart/form-data`. Supply an `avatar` file part containing JPEG/PNG bytes, ≤ 5 MB, to set or replace the avatar. Omit the `avatar` part to remove the avatar. To keep it on a full update, resend the current image as a file. A supplied file must be non-empty and contain a valid JPEG/PNG image. The response is JSON `UserResponse`, including `avatar` with a presigned URL for the saved image or `null` after removal.

For removal, include the text form field `_charset_=UTF-8` to send a valid multipart body while omitting `avatar`. Malformed multipart bodies and invalid images return 400; files larger than 5 MB return 413.

Image uploads and migration of existing database avatars can return 503 when file storage is unavailable. An existing MinIO object key can be signed locally even during a storage outage; fetching its URL requires MinIO to be available. A refresh that fails during avatar migration leaves the submitted refresh token usable for a retry.

Set `MINIO_PUBLIC_ENDPOINT` to the S3 API address reachable by the frontend, including its scheme and port. Backend requests use `MINIO_ENDPOINT`, which can be an internal Docker address. The host in a returned signed URL must be used as generated.

### 1.2 Admin
| # | Method | Path | Acc | Body | Returns | Notes |
|---|---|---|---|---|---|---|
| 7 | POST | `/admin/artists` | A | `ArtistRequest` | 201 `{id}` | `name` ≤ 255 |
| 8 | POST | `/admin/genres` | A | `GenreRequest` | 201 `{id}` | `name` ≤ 100; 409 if the genre exists |
| 9 | POST | `/admin/releases` | A | `ReleaseRequest` | 201 `{id}` | `artistIds` 1–20; `genreIds` 0–20 |
| 10 | POST | `/admin/releases/{id}/songs` | A | multipart `UploadSongRequest` | 201 `{id}` | `trackNumber` ≥ 1, 409 if taken; `title` ≤ 255; `file`: FLAC 16-bit, ≤ 48 kHz, mono/stereo, ≤ 200 MB, ≤ 7200 s; max 2 concurrent → 429 |

### 1.3 Catalog
| # | Method | Path | Acc | Body | Returns | Notes |
|---|---|---|---|---|---|---|
| 11 | GET | `/releases/{id}` | U | | `ReleaseResponse` | |
| 12 | GET | `/playlists/{id}` | U | | `PlaylistResponse` | Private: owner only, else 404 |
| 13 | GET | `/artists/{id}` | U | | `ArtistResponse` | |
| 14 | GET | `/genres` | U | | `GenreResponse[]` | Not paged |
| 15 | GET | `/songs/{id}/audio/{quality:low\|medium\|high\|lossless}` | U | | 200 / 206 audio | Optional `Range`; 416 bad range |
| 16 | GET | `/search{?q,page,size}` | U | | `PageResponse<SearchItemResponse>` | All types mixed |
| 17 | GET | `/search/songs{?q,page,size}` | U | | `PageResponse<SongResponse>` | |
| 18 | GET | `/search/releases{?q,page,size}` | U | | `PageResponse<ReleaseSummaryResponse>` | |
| 19 | GET | `/search/playlists{?q,page,size}` | U | | `PageResponse<PlaylistSummaryResponse>` | |
| 20 | GET | `/search/artists{?q,page,size}` | U | | `PageResponse<ArtistSummaryResponse>` | |

<sub>Audio: `low.m4a` HE-AAC 64 kbps · `medium.m4a` AAC-LC 128 kbps · `high.m4a` AAC-LC 256 kbps · `lossless.flac` original. Search: `q` 1–100 chars, best match first; playlists: public or the caller's own.</sub>

### 1.4 Library, playlists, comments
| # | Method | Path | Acc | Body | Returns | Notes |
|---|---|---|---|---|---|---|
| 21 | GET | `/library{?q,sort,page,size}` | U | | `PageResponse<LibraryItemResponse>` | Saved releases + playlists |
| 22 | GET | `/library/releases{?q,sort,page,size}` | U | | `PageResponse<ReleaseSummaryResponse>` | |
| 23 | GET | `/library/playlists{?q,owned,sort,page,size}` | U | | `PageResponse<PlaylistSummaryResponse>` | `owned=true`: only the caller's own |
| 24 | GET | `/library/artists{?q,sort,page,size}` | U | | `PageResponse<ArtistSummaryResponse>` | No `ARTIST` sort |
| 25 | GET | `/library/songs{?q,sort,page,size}` | U | | `PageResponse<SongResponse>` | |
| 26 | PUT | `/library/{type:releases\|playlists\|artists\|songs}/{id}` | U | | 204 | Save, follow, favorite; idempotent |
| 27 | DELETE | `/library/{type:releases\|playlists\|artists\|songs}/{id}` | U | | 204 | Idempotent; 400 for the caller's own playlist (delete it with #30) |
| 28 | POST | `/playlists` | U | `PlaylistRequest` | 201 `PlaylistResponse` | Max 200 owned; also added to the caller's library |
| 29 | PUT | `/playlists/{id}` | U owner | `PlaylistRequest` | 204 | |
| 30 | DELETE | `/playlists/{id}` | U owner | | 204 | |
| 31 | PUT | `/playlists/{id}/songs/{songId}` | U owner | | 204 | Appended; idempotent; max 1000 |
| 32 | DELETE | `/playlists/{id}/songs/{songId}` | U owner | | 204 | |
| 33 | PUT | `/playlists/{id}/songs` | U owner | `UUID[]` | 204 | Reorder; must be exactly the playlist's songs, else 400 |
| 34 | GET | `/songs/{id}/comments{?page,size}` | U | | `PageResponse<CommentResponse>` | `size` default 10; top-level, newest first |
| 35 | GET | `/comments/{id}/replies{?page,size}` | U | | `PageResponse<CommentResponse>` | `size` default 10; oldest first |
| 36 | POST | `/songs/{id}/comments` | U | `CommentRequest` | 201 `CommentResponse` | `content` ≤ 2000 |
| 37 | POST | `/comments/{id}/replies` | U | `CommentRequest` | 201 `CommentResponse` | `content` ≤ 2000; parent must be top-level, else 400 |
| 38 | DELETE | `/comments/{id}` | U author / A | | 204 | Also deletes its replies |

<sub>Library: `q` 1–100 chars, filters by title/name; `sort`: `RECENT` (default, newest first) \| `NAME` \| `ARTIST` (first artist; playlists: owner username).</sub>

## 2. Objects
```
RegisterRequest         { username, email, password }
LoginRequest            { usernameOrEmail, password }
RefreshTokenRequest     { refreshToken }
UpdateProfileRequest    { avatar?: file }                               // multipart; omitted = remove
PlaylistRequest         { title, isPrivate }
CommentRequest          { content }
ArtistRequest           { name, avatarDataUri: string|null }
GenreRequest            { name }
ReleaseRequest          { title, releaseType, releaseDate, artistIds, genreIds, coverDataUri: string|null }
UploadSongRequest       { trackNumber, title, file }

AuthResponse            { accessToken, refreshToken, user: UserResponse }
UserResponse            { id, username, email, createdAt, role: USER|ADMIN, avatar: string|null }
UserSummaryResponse     { id, username, avatar: string|null }
PageResponse<T>         { content: T[], hasNext }

ArtistSummaryResponse   { id, name, avatar: string|null }
ArtistResponse          { ...ArtistSummaryResponse, isFollowed, releases: ReleaseSummaryResponse[] }
GenreResponse           { id, name, slug }

ReleaseSummaryResponse  { id, title, cover: string|null, releaseType: ALBUM|SINGLE|EP|COMPILATION, releaseDate, songCount, artists: ArtistSummaryResponse[], isSaved }
ReleaseResponse         { ...ReleaseSummaryResponse, songs: SongSummaryResponse[] }

SongSummaryResponse     { id, title, durationSeconds, trackNumber, artists: ArtistSummaryResponse[], isFavorited }
SongResponse            { ...SongSummaryResponse, release: { id, title, cover: string|null } }

PlaylistSummaryResponse { id, title, cover: string|null, isPrivate, owner: UserSummaryResponse, songCount, isSaved }
PlaylistResponse        { ...PlaylistSummaryResponse, songs: SongResponse[] }

CommentResponse         { id, content, commentedAt, user: UserSummaryResponse, replyCount }

SearchItemResponse      { type: "SONG", ...SongResponse } | { type: "RELEASE", ...ReleaseSummaryResponse } | { type: "PLAYLIST", ...PlaylistSummaryResponse } | { type: "ARTIST", ...ArtistSummaryResponse }
LibraryItemResponse     { type: "RELEASE", ...ReleaseSummaryResponse } | { type: "PLAYLIST", ...PlaylistSummaryResponse }
```
