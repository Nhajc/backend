# Nhajc API

- `/api/v1` · port 8080 · JSON (multipart where marked) · errors: RFC 9457 `ProblemDetail`
- auth: `Bearer <accessToken>` (15 min); refresh token 30 days, rotated each use, reuse revokes its family.
- Access: **P** public · **U** user · **A** admin.
- List paging: zero-based `page=0`, `size=20` (1–100; comments default 10). Returns `{content, page, size, hasNext}` (`page` and `size` echo the requested pagination, including empty pages); `hasNext=true` means another page exists. Negative page, size outside 1–100 → 400. A page past the end returns `{content: [], page, size, hasNext: false}`. Each list defines a stable order; genre lists use case-insensitive name then ID. Enums are UPPERCASE.
- JSON requests: send every field. `T?` = may be `null`; in multipart, `null` = leave the part out. `PUT` replaces the whole object.
- All multipart requests contain `data` (required, Content-Type: application/json, the request object below) and `file` (binary). The file is optional for images and required for song upload. Images: JPEG/PNG ≤ 5 MiB. 400 invalid image, 413 too large, 503 storage unavailable. On PUT, omitting `file` removes the avatar (#6) but keeps the playlist cover (#29).
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
| 6 | PUT | `/users/me` | U | multipart `UpdateProfileRequest` | 200 `UserResponse` | `data={}`; optional `file` sets avatar; omitted file removes it |

- Registration: username 3–50 characters, no `@` or whitespace; email valid and ≤ 254 characters; password 12–72 characters and ≤ 72 UTF-8 bytes. Username/email are stripped before validation; passwords are preserved. Invalid input → 400, identifiers already taken (case-insensitive) → 409.
- Login: username/email are case-insensitive; the stripped identifier is at most 254 characters. An identifier containing `@` is an email; otherwise it is a username. Invalid credentials return 401.
- Unexpected server failures return 500 with a generic `ProblemDetail`.

### 1.2 Admin

Admin catalog lists:

- `GET /admin/artists{?q,page,size}` → `PageResponse<ArtistSummaryResponse>` (ADMIN only).
- `GET /admin/releases{?q,page,size}` → `PageResponse<ReleaseSummaryResponse>` (ADMIN only).
- Omit `q`, or send an empty/whitespace query, to browse the full list. A nonblank
  query filters names/titles using the search matching rules; maximum 100 characters.
  Unfiltered lists sort by case-insensitive name/title and ID. Pagination uses the
  same envelope and limits as other list endpoints. The existing `/search` endpoints
  still require a nonblank keyword.

| # | Method | Path | Acc | Body | Returns | Notes |
|---|---|---|---|---|---|---|
| 7 | POST | `/admin/artists` | A | multipart `ArtistRequest` | 201 `{id}` | `name` ≤ 255 |
| 8 | POST | `/admin/genres` | A | `GenreRequest` | 201 `{id}` | `name` ≤ 100; 409 if the genre exists |
| 9 | POST | `/admin/releases` | A | multipart `ReleaseRequest` | 201 `{id}` | `artistIds` 1–20, in credit order; `genreIds` 0–20 |
| 10 | POST | `/admin/releases/{id}/songs` | A | multipart `UploadSongRequest` | 201 `{id}` | Required client-provided `trackNumber` ≥ 1; missing/null/invalid → 400, already taken → 409; `title` ≤ 255; `file`: FLAC 16-bit, ≤ 48 kHz, mono/stereo, ≤ 200 MB, ≤ 7200 s; 429 when processing capacity is exceeded |

- Release metadata is JSON: `artistIds` and `genreIds` are UUID arrays; `releaseDate` is an ISO date (`yyyy-MM-dd`). Omit `genreIds` or use an empty array for none.
- Names/titles are stripped before validation. Genre names are unique ignoring case. Release title ≤ 255; duplicate artist/genre IDs → 400; missing referenced IDs → 404. Creation returns `IdResponse(UUID id)`, serialized as `{ "id": "..." }`.
- Artist/release images are normalized to at most 600 px; input is limited to 5 MiB and 40 million pixels. The optional `file` supplies the artist avatar or release cover.
- Song upload is synchronous: 201 only after all four qualities are stored. Artists are inherited from the release. A server process supports two concurrent upload/processing requests; additional requests return 429 before processing. Clients should queue remaining files and hold each slot until the HTTP response arrives. Processing unavailable → 503; unencodable AAC profile → 422. FLAC embedded JPEG/PNG becomes the release cover only if no cover exists.
- Direct song uploads require an explicit positive `trackNumber` from the client. The backend stores that exact number and checks for duplicates under a release row lock after processing; an occupied number returns 409. Missing/null/zero/negative numbers return 400 before processing. The backend does not infer an append position for this endpoint.
- `PUT /admin/releases/{id}/songs/order` (ADMIN only) accepts JSON `{ "songIds": ["uuid", "uuid"] }` containing every current song ID in the desired order and returns 204. It assigns track numbers 1..N atomically under the same release lock as uploads. Null/duplicate IDs return 400, an incomplete/stale/foreign set returns 409, and a missing release returns 404. An empty list is valid only for an empty release. Migration V10 defers track-number uniqueness within the reorder transaction to support swaps without temporary invalid numbers.

Upload-and-review flow (ADMIN only; the Android UI uses this flow instead of direct publication):

- `POST /admin/releases/{id}/song-uploads` with one multipart `file` prepares all four qualities using the shared two-slot processing limit. Returns 201 `{id,durationSeconds,expiresAt}`. It does not create a song, update release cover, or expose search/playback. A draft belongs to the authenticated actor and release and expires one hour after processing.
- `PUT /admin/releases/{id}/song-uploads` accepts `{songs:[{id,title,description,artistIds}],songIds:[...]}`. `songs` contains the prepared uploads being published; titles are 1–255 characters, optional descriptions up to 4,000, and artist IDs 1–20 unique entries in credit order. `songIds` contains every existing and new song ID in the desired final order. Returns 204 after publishing metadata, media, artist credits and tracks 1..N in one transaction under the release lock. 400 invalid/duplicate metadata; 404 unknown release/artist; 409 expired/foreign/consumed drafts or a stale/incomplete song list. Failed validation publishes no part of the batch. Repeating a consumed batch returns 409 without creating duplicates.
- `DELETE /admin/releases/{id}/song-uploads/{uploadId}` discards only the actor's own uncommitted draft and returns 204; repeating it is harmless. Temporary media has cleanup reservations; discarding accelerates cleanup, and abandoned drafts/media expire automatically. Migration V11 creates the staging table and adds nullable `description` to songs; song summary responses include that description.

### 1.3 Catalog
| # | Method | Path | Acc | Body | Returns | Notes |
|---|---|---|---|---|---|---|
| 11 | GET | `/releases/{id}` | U | | `ReleaseResponse` | All songs, track number ascending |
| 12 | GET | `/playlists/{id}` | U | | `PlaylistResponse` | All songs in playlist order; private: owner only, else 404 |
| 13 | GET | `/artists/{id}` | U | | `ArtistResponse` | All releases, newest release date first |
| 14 | GET | `/genres{?page,size}` | U | | `PageResponse<GenreResponse>` | Default `page=0,size=20`; name ascending ignoring case, ID as tie-breaker |
| 15 | GET | `/songs/{id}/audio/{quality:low\|medium\|high\|lossless}` | U | | 302 | `Location` = presigned audio URL; follow it (it supports `Range`) |
| 16 | GET | `/search{?q,page,size}` | U | | `PageResponse<TypedItemResponse>` | All types mixed |
| 17 | GET | `/search/songs{?q,page,size}` | U | | `PageResponse<SongResponse>` | |
| 18 | GET | `/search/releases{?q,page,size}` | U | | `PageResponse<ReleaseSummaryResponse>` | |
| 19 | GET | `/search/playlists{?q,page,size}` | U | | `PageResponse<PlaylistSummaryResponse>` | |
| 20 | GET | `/search/artists{?q,page,size}` | U | | `PageResponse<ArtistSummaryResponse>` | |

<sub>Audio: `low.m4a` HE-AAC 64 kbps · `medium.m4a` AAC-LC 128 kbps · `high.m4a` AAC-LC 256 kbps · `lossless.flac` original. Search: `q` 1–100 chars, exact title/name first, then prefix, then substring (case-insensitive; `%` and `_` are literal); ties: name, type, ID; playlists: public or the caller's own.</sub>

### 1.4 Library, playlists, comments
| # | Method | Path | Acc | Body | Returns | Notes |
|---|---|---|---|---|---|---|
| 21 | GET | `/library{?page,size}` | U | | `PageResponse<TypedItemResponse>` | Saved releases + playlists (`type` only `RELEASE` or `PLAYLIST`) |
| 22 | GET | `/library/releases{?page,size}` | U | | `PageResponse<ReleaseSummaryResponse>` | |
| 23 | GET | `/library/playlists{?page,size}` | U | | `PageResponse<PlaylistSummaryResponse>` | |
| 24 | GET | `/library/artists{?page,size}` | U | | `PageResponse<ArtistSummaryResponse>` | |
| 25 | GET | `/library/songs{?page,size}` | U | | `PageResponse<SongResponse>` | |
| 26 | PUT | `/library/{type:releases\|playlists\|artists\|songs}/{id}` | U | | 204 | Save, follow, favorite; idempotent |
| 27 | DELETE | `/library/{type:releases\|playlists\|artists\|songs}/{id}` | U | | 204 | Idempotent; 400 for the caller's own playlist (delete it with #30) |
| 28 | POST | `/playlists` | U | multipart `PlaylistRequest` | 201 `PlaylistResponse` | Title 1–255 chars; `songIds` 0–1000 in playlist order, no repeats (400), unknown song 404; max 200 owned (409 if full); also added to the caller's library |
| 29 | PUT | `/playlists/{id}` | U owner | multipart `PlaylistRequest` | 204 | Replaces title, privacy and songs (`songIds` as in #28). Omitted `file` keeps the current cover; a cover can be replaced, not removed. Making it private removes it from other users' libraries |
| 30 | DELETE | `/playlists/{id}` | U owner | | 204 | |
| 31 | PUT | `/playlists/{id}/songs/{songId}` | U owner | | 204 | Appended; idempotent; max 1000 (409 if full) |
| 32 | GET | `/songs/{id}/comments{?page,size}` | U | | `PageResponse<CommentResponse>` | `size` default 10; top-level, newest first |
| 33 | GET | `/comments/{id}/replies{?page,size}` | U | | `PageResponse<CommentResponse>` | `size` default 10; oldest first |
| 34 | POST | `/songs/{id}/comments` | U | `CommentRequest` | 201 `CommentResponse` | `content` ≤ 2000 |
| 35 | POST | `/comments/{id}/replies` | U | `CommentRequest` | 201 `CommentResponse` | `content` ≤ 2000; parent must be top-level, else 400 |
| 36 | DELETE | `/comments/{id}` | U author / A | | 204 | Also deletes its replies |

<sub>Library lists: newest first by `lastActivityAt` / `followedAt` / `favoritedAt`, then name, then ID.</sub>

## 2. Objects
```
RegisterRequest         { username, email, password }
LoginRequest            { usernameOrEmail, password }
RefreshTokenRequest     { refreshToken }
UpdateProfileRequest    {}
PlaylistRequest         { title, isPrivate: boolean, songIds: UUID[] }
CommentRequest          { content }
ArtistRequest           { name }
GenreRequest            { name }
ReleaseRequest          { title, releaseType, releaseDate, artistIds: UUID[], genreIds?: UUID[] }
UploadSongRequest       { trackNumber: positive integer, title }

AuthResponse            { accessToken, refreshToken, user: UserResponse }
UserResponse            { id, username, email, createdAt, role: USER|ADMIN, avatarUrl: string? }
UserSummaryResponse     { id, username, avatarUrl: string? }
PageResponse<T>         { content: T[], page: int, size: int, hasNext: boolean }

ArtistSummaryResponse   { id, name, avatarUrl: string?, followedAt: Instant? }
ArtistResponse          { ...ArtistSummaryResponse, releases: ReleaseSummaryResponse[] }
GenreResponse           { id, name, slug }

ReleaseSummaryResponse  { id, title, coverUrl: string?, releaseType: ALBUM|SINGLE|EP|COMPILATION, releaseDate, songCount, artists: ArtistSummaryResponse[], lastActivityAt: Instant? }
ReleaseResponse         { ...ReleaseSummaryResponse, songs: SongSummaryResponse[] }

SongSummaryResponse     { id, title, durationSeconds, trackNumber, artists: ArtistSummaryResponse[], favoritedAt: Instant? }
SongResponse            { ...SongSummaryResponse, releaseId, releaseTitle, coverUrl: string? }

PlaylistSummaryResponse { id, title, coverUrl: string?, isPrivate, owner: UserSummaryResponse, songCount, lastActivityAt: Instant? }
PlaylistResponse        { ...PlaylistSummaryResponse, songs: SongResponse[] }

CommentResponse         { id, content, commentedAt, user: UserSummaryResponse, replyCount }

TypedItemResponse       { type: "SONG", ...SongResponse } | { type: "RELEASE", ...ReleaseSummaryResponse } | { type: "PLAYLIST", ...PlaylistSummaryResponse } | { type: "ARTIST", ...ArtistSummaryResponse }
```

Every `artists` list is in credit order (the order of #9 `artistIds`).

Library timestamps are UTC instants: `null` means not saved, followed or favorited. Opening a saved release or playlist updates `lastActivityAt`.
