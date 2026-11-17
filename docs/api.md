# Nhajc API

- `/api/v1` · port 8080 · JSON · errors: RFC 9457 `ProblemDetail`
- auth: `Bearer <accessToken>` (15 min); refresh token 30 days, rotated each use, reuse revokes its family.
- Access: **P** public · **U** user · **A** admin.
- Paging: `page=0`, `size=20` (1–100). Enums are UPPERCASE. Images are fetched by id; 404 = no image.

## 1. Endpoints

### 1.1 Auth / user
| # | Method | Path | Acc | Body | Returns | Notes |
|---|---|---|---|---|---|---|
| 1 | POST | `/auth/register` | P | `RegisterRequest` | 201 `AuthResponse` | 409 username or email taken |
| 2 | POST | `/auth/login` | P | `LoginRequest` | `AuthResponse` | 401 bad credentials |
| 3 | POST | `/auth/refresh` | P | `RefreshTokenRequest` | `AuthResponse` | |
| 4 | POST | `/auth/logout` | P | `RefreshTokenRequest` | 204 | |
| 5 | GET | `/users/me` | U | | `UserResponse` | |
| 6 | PUT | `/users/me/avatar` | U | `image/jpeg\|png` | 204 | Raw image body, ≤ 5 MB; replaces the old one |
| 7 | DELETE | `/users/me/avatar` | U | | 204 | Idempotent |
| 8 | GET | `/users/{id}/avatar` | U | | `image/jpeg\|png` | |

### 1.2 Admin
| # | Method | Path | Acc | Body | Returns | Notes |
|---|---|---|---|---|---|---|
| 9 | POST | `/admin/artists` | A | `ArtistRequest` | 201 `{id}` | `name` ≤ 255 |
| 10 | PUT | `/admin/artists/{id}/avatar` | A | `image/jpeg\|png` | 204 | Raw image body, ≤ 5 MB; replaces the old one |
| 11 | POST | `/admin/genres` | A | `GenreRequest` | 201 `{id}` | `name` ≤ 100; 409 if the genre exists |
| 12 | POST | `/admin/releases` | A | `ReleaseRequest` | 201 `{id}` | `artistIds` 1–20; `genreIds` 0–20 |
| 13 | PUT | `/admin/releases/{id}/cover` | A | `image/jpeg\|png` | 204 | Raw image body, ≤ 5 MB; replaces the old one |
| 14 | POST | `/admin/releases/{id}/songs` | A | multipart `UploadSongRequest` | 201 `{id}` | `trackNumber` ≥ 1, 409 if taken; `title` ≤ 255; `file`: FLAC 16-bit, ≤ 48 kHz, mono/stereo, ≤ 200 MB, ≤ 7200 s; max 2 concurrent → 429 |

### 1.3 Catalog
| # | Method | Path | Acc | Body | Returns | Notes |
|---|---|---|---|---|---|---|
| 15 | GET | `/releases/{id}` | U | | `ReleaseResponse` | |
| 16 | GET | `/releases/{id}/cover` | P | | `image/jpeg\|png` | |
| 17 | GET | `/playlists/{id}` | U | | `PlaylistResponse` | Private: owner only, else 404 |
| 18 | GET | `/playlists/{id}/cover` | U | | `image/jpeg\|png` | Private: owner only, else 404 |
| 19 | GET | `/artists/{id}` | U | | `ArtistResponse` | |
| 20 | GET | `/artists/{id}/avatar` | P | | `image/jpeg\|png` | |
| 21 | GET | `/genres` | U | | `GenreResponse[]` | Not paged |
| 22 | GET | `/songs/{id}/audio/{quality:low\|medium\|high\|lossless}` | U | | 200 / 206 audio | Optional `Range`; 416 bad range |
| 23 | GET | `/search{?q,page,size}` | U | | `PageResponse<SearchItemResponse>` | All types mixed |
| 24 | GET | `/search/songs{?q,page,size}` | U | | `PageResponse<SongResponse>` | |
| 25 | GET | `/search/releases{?q,page,size}` | U | | `PageResponse<ReleaseSummaryResponse>` | |
| 26 | GET | `/search/playlists{?q,page,size}` | U | | `PageResponse<PlaylistSummaryResponse>` | |
| 27 | GET | `/search/artists{?q,page,size}` | U | | `PageResponse<ArtistSummaryResponse>` | |

<sub>Audio: `low.m4a` HE-AAC 64 kbps · `medium.m4a` AAC-LC 128 kbps · `high.m4a` AAC-LC 256 kbps · `lossless.flac` original. Search: `q` 1–100 chars, best match first; playlists: public or the caller's own.</sub>

### 1.4 Library, playlists, comments
| # | Method | Path | Acc | Body | Returns | Notes |
|---|---|---|---|---|---|---|
| 28 | GET | `/library{?q,sort,page,size}` | U | | `PageResponse<LibraryItemResponse>` | Saved releases + playlists |
| 29 | GET | `/library/releases{?q,sort,page,size}` | U | | `PageResponse<ReleaseSummaryResponse>` | |
| 30 | GET | `/library/playlists{?q,owned,sort,page,size}` | U | | `PageResponse<PlaylistSummaryResponse>` | `owned=true`: only the caller's own |
| 31 | GET | `/library/artists{?q,sort,page,size}` | U | | `PageResponse<ArtistSummaryResponse>` | No `ARTIST` sort |
| 32 | GET | `/library/songs{?q,sort,page,size}` | U | | `PageResponse<SongResponse>` | |
| 33 | PUT | `/library/{type:releases\|playlists\|artists\|songs}/{id}` | U | | 204 | Save, follow, favorite; idempotent |
| 34 | DELETE | `/library/{type:releases\|playlists\|artists\|songs}/{id}` | U | | 204 | Idempotent; 400 for the caller's own playlist (delete it with #37) |
| 35 | POST | `/playlists` | U | `PlaylistRequest` | 201 `PlaylistResponse` | Max 200 owned; also added to the caller's library |
| 36 | PUT | `/playlists/{id}` | U owner | `PlaylistRequest` | 204 | |
| 37 | DELETE | `/playlists/{id}` | U owner | | 204 | |
| 38 | PUT | `/playlists/{id}/songs/{songId}` | U owner | | 204 | Appended; idempotent; max 1000 |
| 39 | DELETE | `/playlists/{id}/songs/{songId}` | U owner | | 204 | |
| 40 | PUT | `/playlists/{id}/songs` | U owner | `UUID[]` | 204 | Reorder; must be exactly the playlist's songs, else 400 |
| 41 | GET | `/songs/{id}/comments{?page,size}` | U | | `PageResponse<CommentResponse>` | `size` default 10; top-level, newest first |
| 42 | GET | `/comments/{id}/replies{?page,size}` | U | | `PageResponse<CommentResponse>` | `size` default 10; oldest first |
| 43 | POST | `/songs/{id}/comments` | U | `CommentRequest` | 201 `CommentResponse` | `content` ≤ 2000 |
| 44 | POST | `/comments/{id}/replies` | U | `CommentRequest` | 201 `CommentResponse` | `content` ≤ 2000; parent must be top-level, else 400 |
| 45 | DELETE | `/comments/{id}` | U author / A | | 204 | Also deletes its replies |

<sub>Library: `q` 1–100 chars, filters by title/name; `sort`: `RECENT` (default, newest first) \| `NAME` \| `ARTIST` (first artist; playlists: owner username).</sub>

## 2. Objects
```
RegisterRequest         { username, email, password }
LoginRequest            { usernameOrEmail, password }
RefreshTokenRequest     { refreshToken }
PlaylistRequest         { title, isPrivate }
CommentRequest          { content }
ArtistRequest           { name }
GenreRequest            { name }
ReleaseRequest          { title, releaseType, releaseDate, artistIds, genreIds }
UploadSongRequest       { trackNumber, title, file }                     // multipart

AuthResponse            { accessToken, refreshToken, user: UserResponse }
UserResponse            { id, username, email, createdAt, role: USER|ADMIN }
UserSummaryResponse     { id, username }
PageResponse<T>         { content: T[], hasNext }

ArtistSummaryResponse   { id, name }
ArtistResponse          { ...ArtistSummaryResponse, isFollowed, releases: ReleaseSummaryResponse[] }
GenreResponse           { id, name, slug }

ReleaseSummaryResponse  { id, title, releaseType: ALBUM|SINGLE|EP|COMPILATION, releaseDate, songCount,
                          artists: ArtistSummaryResponse[], isSaved }
ReleaseResponse         { ...ReleaseSummaryResponse, songs: SongResponse[] }

SongResponse            { id, title, durationSeconds, trackNumber, release: { id, title },
                          artists: ArtistSummaryResponse[], isFavorited }

PlaylistSummaryResponse { id, title, isPrivate, owner: UserSummaryResponse, songCount, isSaved }
PlaylistResponse        { ...PlaylistSummaryResponse, songs: SongResponse[] }

CommentResponse         { id, content, commentedAt, user: UserSummaryResponse, replyCount }

SearchItemResponse      { type: "SONG", ...SongResponse } | { type: "RELEASE", ...ReleaseSummaryResponse }
                        | { type: "PLAYLIST", ...PlaylistSummaryResponse } | { type: "ARTIST", ...ArtistSummaryResponse }
LibraryItemResponse     { type: "RELEASE", ...ReleaseSummaryResponse } | { type: "PLAYLIST", ...PlaylistSummaryResponse }
```
