# Nhajc API

- `/api/v1` · port 8080 · JSON (multipart where marked) · errors: RFC 9457 `ProblemDetail`
- auth: `Bearer <accessToken>` (15 min); refresh token 30 days, rotated each use, reuse revokes its family.
- Access: **P** public · **U** user · **A** admin.
- List paging: zero-based `page=0`, `size=20` (1–100; comments default 10). Returns `{content, page, size, hasNext}` (`page` and `size` echo the requested pagination, including empty pages); `hasNext=true` means another page exists. Negative page, size outside 1–100 or offset above 2147483647 → 400. A page past the end returns `{content: [], page, size, hasNext: false}`. Each list defines a stable order; genre lists use case-insensitive name then ID. Enums are UPPERCASE.
- JSON requests: send every field. `T?` = may be `null`; in multipart, `null` = leave the part out. `PUT` replaces the whole object.
- All image uploads use `multipart/form-data`: JPEG/PNG file parts ≤ 5 MiB; text metadata is sent in the same request. Omit an optional image part when there is no image; do not send a text filename/path, Base64 or the string `null`. Let the client generate the Content-Type boundary. 400 invalid image, 413 too large, 503 storage down.
- Image responses: `avatarUrl` / `coverUrl` = presigned URL, used as given, or `null`. Valid 1 h; refetch the resource for a new one.

## 1. Endpoints

### 1.1 Auth / user
| # | Method | Path | Acc | Body | Returns | Notes |
|---|---|---|---|---|---|---|
| 1 | POST | `/auth/register` | P | `RegisterRequest` | 201 `AuthResponse` | 409 username or email taken |
| 2 | POST | `/auth/login` | P | `LoginRequest` | `AuthResponse` | 401 bad credentials |
| 3 | POST | `/auth/refresh` | P | `RefreshTokenRequest` | `AuthResponse` | |
| 4 | POST | `/auth/logout` | P | `RefreshTokenRequest` | 204 | |
| 5 | GET | `/users/me` | U | | `UserResponse` | |
| 6 | PUT | `/users/me` | U | multipart `UpdateProfileRequest` | 200 `UserResponse` | Only the avatar for now. `avatar` part = set; no `avatar` part (send `_charset_=UTF-8` so the body isn't empty) = remove |

- Profile update: `avatar` must be a file part, not a text field containing a filename/path. If both a file and text field named `avatar` are present, the file is used. A text-only `avatar` returns 400 and leaves the current avatar unchanged.
- Registration: username 3–50 characters, no `@` or whitespace; email valid and ≤ 254 characters; password 12–72 characters and ≤ 72 UTF-8 bytes. Username/email are stripped before validation; passwords are preserved. Invalid input → 400, identifiers already taken (including legacy username/email collisions) → 409.
- Login: username/email are case-insensitive; the stripped identifier is ≤ 254 characters. Inputs containing `@` resolve to email first, with a legacy username fallback only if that email does not exist. Existing collisions resolve to the email owner; the legacy username owner can use their own email. New usernames cannot contain `@`. Existing shorter passwords still work. Missing account, wrong password and password over 72 UTF-8 bytes all return the same 401 message.
- Unexpected server failures return 500 with a generic `ProblemDetail`; details are logged on the server.

### 1.2 Admin

Implemented: all endpoints #1–38. See [Admin setup](admin.md) and [Catalog, library and pagination rules](catalog-library.md).

| # | Method | Path | Acc | Body | Returns | Notes |
|---|---|---|---|---|---|---|
| 7 | POST | `/admin/artists` | A | multipart `ArtistRequest` | 201 `{id}` | `name` ≤ 255 |
| 8 | POST | `/admin/genres` | A | `GenreRequest` | 201 `{id}` | `name` ≤ 100; 409 if the genre exists |
| 9 | POST | `/admin/releases` | A | multipart `ReleaseRequest` | 201 `{id}` | `artistIds` 1–20; `genreIds` 0–20 |
| 10 | POST | `/admin/releases/{id}/songs` | A | multipart `UploadSongRequest` | 201 `{id}` | `trackNumber` ≥ 1, 409 if taken; `title` ≤ 255; `file`: FLAC 16-bit, ≤ 48 kHz, mono/stereo, ≤ 200 MB, ≤ 7200 s; max 2 concurrent → 429 |

- Multipart release fields: `title`, `releaseType` and ISO `releaseDate` (`yyyy-MM-dd`) are Text. Repeat Text key `artistIds` or `genreIds` once per UUID; no JSON arrays. At least one artist is required; omit `genreIds` for none.
- Names/titles are stripped before validation. Genre names are unique ignoring case. Release title ≤ 255; duplicate artist/genre IDs → 400; missing referenced IDs → 404. Creation returns `IdResponse(UUID id)`, serialized as `{ "id": "..." }`.
- Artist/release images are normalized to at most 600 px; input is limited to 5 MiB and 40 million pixels. Image parts are optional: `avatar` for artist, `cover` for release. Text fields named avatar/cover without a file return 400.
- Song upload is synchronous: 201 only after all four qualities are stored. Artists are inherited from the release. Maximum 2 processing tasks per server instance. Failed processing does not publish the song. Missing tools/storage or timeout → 503; unencodable AAC profile → 422. FLAC embedded JPEG/PNG becomes the release cover only if no cover exists.

### 1.3 Catalog
| # | Method | Path | Acc | Body | Returns | Notes |
|---|---|---|---|---|---|---|
| 11 | GET | `/releases/{id}{?page,size}` | U | | `ReleaseResponse` | Paged songs, track number ascending |
| 12 | GET | `/playlists/{id}{?page,size}` | U | | `PlaylistResponse` | Paged songs in playlist order; private: owner only, else 404 |
| 13 | GET | `/artists/{id}{?page,size}` | U | | `ArtistResponse` | Paged releases, newest release date first |
| 14 | GET | `/genres{?page,size}` | U | | `PageResponse<GenreResponse>` | Default `page=0,size=20`; name ascending ignoring case, ID as tie-breaker |
| 15 | GET | `/songs/{id}/audio/{quality:low\|medium\|high\|lossless}` | U | | 302 | `Location` = presigned audio URL; follow it (it supports `Range`) |
| 16 | GET | `/search{?q,page,size}` | U | | `PageResponse<SearchItemResponse>` | All types mixed |
| 17 | GET | `/search/songs{?q,page,size}` | U | | `PageResponse<SongResponse>` | |
| 18 | GET | `/search/releases{?q,page,size}` | U | | `PageResponse<ReleaseSummaryResponse>` | |
| 19 | GET | `/search/playlists{?q,page,size}` | U | | `PageResponse<PlaylistSummaryResponse>` | |
| 20 | GET | `/search/artists{?q,page,size}` | U | | `PageResponse<ArtistSummaryResponse>` | |

<sub>Audio: `low.m4a` HE-AAC 64 kbps · `medium.m4a` AAC-LC 128 kbps · `high.m4a` AAC-LC 256 kbps · `lossless.flac` original. Search: `q` 1–100 chars, exact title/name first, then prefix, then substring (case-insensitive; `%` and `_` are literal); ties: name, type, ID; playlists: public or the caller's own.</sub>

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
| 28 | POST | `/playlists` | U | `PlaylistRequest` | 201 `PlaylistResponse` | Title 1–255 chars; max 200 owned (409 if full); also added to the caller's library |
| 29 | PUT | `/playlists/{id}` | U owner | `PlaylistRequest` | 204 | |
| 30 | DELETE | `/playlists/{id}` | U owner | | 204 | |
| 31 | PUT | `/playlists/{id}/songs/{songId}` | U owner | | 204 | Appended; idempotent; max 1000 (409 if full) |
| 32 | DELETE | `/playlists/{id}/songs/{songId}` | U owner | | 204 | |
| 33 | PUT | `/playlists/{id}/songs` | U owner | `UUID[]` | 204 | Reorder; must be exactly the playlist's songs, else 400 |
| 34 | GET | `/songs/{id}/comments{?page,size}` | U | | `PageResponse<CommentResponse>` | `size` default 10; top-level, newest first |
| 35 | GET | `/comments/{id}/replies{?page,size}` | U | | `PageResponse<CommentResponse>` | `size` default 10; oldest first |
| 36 | POST | `/songs/{id}/comments` | U | `CommentRequest` | 201 `CommentResponse` | `content` ≤ 2000 |
| 37 | POST | `/comments/{id}/replies` | U | `CommentRequest` | 201 `CommentResponse` | `content` ≤ 2000; parent must be top-level, else 400 |
| 38 | DELETE | `/comments/{id}` | U author / A | | 204 | Also deletes its replies |

<sub>Library: `q` 1–100 chars, optional; when present 1–100 chars, filters by title/name; `sort`: `RECENT` (default, newest first) \| `NAME` \| `ARTIST` (first artist; playlists: owner username).</sub>

All list endpoints accept `page`/`size` and return `PageResponse`. In #11–13, `songs`/`releases` contain that same envelope; the summary fields remain at the top level. Defaults: page 0, size 20 (comments/replies size 10). Relationship metadata such as a release's artists stays a bounded array (max 20).

## 2. Objects
```
RegisterRequest         { username, email, password }
LoginRequest            { usernameOrEmail, password }
RefreshTokenRequest     { refreshToken }
UpdateProfileRequest    { avatar: file? }
PlaylistRequest         { title, isPrivate: boolean }
CommentRequest          { content }
ArtistRequest           { name, avatar: file? }
GenreRequest            { name }
ReleaseRequest          { title, releaseType, releaseDate, artistIds, genreIds?, cover: file? }
UploadSongRequest       { trackNumber, title, file }

AuthResponse            { accessToken, refreshToken, user: UserResponse }
UserResponse            { id, username, email, createdAt, role: USER|ADMIN, avatarUrl: string? }
UserSummaryResponse     { id, username, avatarUrl: string? }
PageResponse<T>         { content: T[], page: int, size: int, hasNext: boolean }

ArtistSummaryResponse   { id, name, avatarUrl: string? }
ArtistResponse          { ...ArtistSummaryResponse, isFollowed, releases: PageResponse<ReleaseSummaryResponse> }
GenreResponse           { id, name, slug }

ReleaseSummaryResponse  { id, title, coverUrl: string?, releaseType: ALBUM|SINGLE|EP|COMPILATION, releaseDate, songCount, artists: ArtistSummaryResponse[], isSaved }
ReleaseResponse         { ...ReleaseSummaryResponse, songs: PageResponse<SongSummaryResponse> }

SongSummaryResponse     { id, title, durationSeconds, trackNumber, artists: ArtistSummaryResponse[], isFavorited }
SongResponse            { ...SongSummaryResponse, releaseId, releaseTitle, coverUrl: string? }

PlaylistSummaryResponse { id, title, coverUrl: string?, isPrivate, owner: UserSummaryResponse, songCount, isSaved }
PlaylistResponse        { ...PlaylistSummaryResponse, songs: PageResponse<SongResponse> }

CommentResponse         { id, content, commentedAt, user: UserSummaryResponse, replyCount }

SearchItemResponse      { type: "SONG", ...SongResponse } | { type: "RELEASE", ...ReleaseSummaryResponse } | { type: "PLAYLIST", ...PlaylistSummaryResponse } | { type: "ARTIST", ...ArtistSummaryResponse }
LibraryItemResponse     { type: "RELEASE", ...ReleaseSummaryResponse } | { type: "PLAYLIST", ...PlaylistSummaryResponse }
```
