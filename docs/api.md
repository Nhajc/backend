# Nhajc API

- `/api/v1` · port 8080 · JSON (multipart where marked) · errors: RFC 9457 `ProblemDetail`
- auth: `Bearer <accessToken>` (15 min); refresh token 30 days, rotated each use, reuse revokes its family.
- Access: **P** public · **U** user · **A** admin.
- Paging: `page=0`, `size=20` (1–100). Enums are UPPERCASE.
- JSON requests: send every field; only `|null` fields may be `null`. `PUT` replaces the whole object.
- Image uploads: JPEG/PNG ≤ 5 MB; in JSON as `data:image/jpeg|png;base64,…` (`null` = no image). 400 invalid image, 413 too large, 503 storage down.
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
| 15 | GET | `/songs/{id}/audio/{quality:low\|medium\|high\|lossless}` | U | | 302 | `Location` = presigned audio URL; follow it (it supports `Range`) |
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
UpdateProfileRequest    { avatar?: file }
PlaylistRequest         { title, isPrivate }
CommentRequest          { content }
ArtistRequest           { name, avatarDataUri: string|null }
GenreRequest            { name }
ReleaseRequest          { title, releaseType, releaseDate, artistIds, genreIds, coverDataUri: string|null }
UploadSongRequest       { trackNumber, title, file }

AuthResponse            { accessToken, refreshToken, user: UserResponse }
UserResponse            { id, username, email, createdAt, role: USER|ADMIN, avatarUrl: string|null }
UserSummaryResponse     { id, username, avatarUrl: string|null }
PageResponse<T>         { content: T[], hasNext }

ArtistSummaryResponse   { id, name, avatarUrl: string|null }
ArtistResponse          { ...ArtistSummaryResponse, isFollowed, releases: ReleaseSummaryResponse[] }
GenreResponse           { id, name, slug }

ReleaseSummaryResponse  { id, title, coverUrl: string|null, releaseType: ALBUM|SINGLE|EP|COMPILATION, releaseDate, songCount, artists: ArtistSummaryResponse[], isSaved }
ReleaseResponse         { ...ReleaseSummaryResponse, songs: SongSummaryResponse[] }

SongSummaryResponse     { id, title, durationSeconds, trackNumber, artists: ArtistSummaryResponse[], isFavorited }
SongResponse            { ...SongSummaryResponse, releaseId, releaseTitle, coverUrl: string|null }

PlaylistSummaryResponse { id, title, coverUrl: string|null, isPrivate, owner: UserSummaryResponse, songCount, isSaved }
PlaylistResponse        { ...PlaylistSummaryResponse, songs: SongResponse[] }

CommentResponse         { id, content, commentedAt, user: UserSummaryResponse, replyCount }

SearchItemResponse      { type: "SONG", ...SongResponse } | { type: "RELEASE", ...ReleaseSummaryResponse } | { type: "PLAYLIST", ...PlaylistSummaryResponse } | { type: "ARTIST", ...ArtistSummaryResponse }
LibraryItemResponse     { type: "RELEASE", ...ReleaseSummaryResponse } | { type: "PLAYLIST", ...PlaylistSummaryResponse }
```
