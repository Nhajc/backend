# Nhajc use cases

- Nhajc is a music streaming app. For now, only Android.
- `#n` = endpoint number in [api.md](api.md).
- Bottom bar: Home, Search, Library, Admin (admins only).
- Top left: my avatar (#8, or the default Material 3 account icon if none) → SettingsScreen.

## 1. Users
| Who | Can do |
|---|---|
| Guest | Register, log in |
| User | Every row in section 2, except Admin |
| Admin | Everything, plus delete any comment |

## 2. Cases
| Screen | Action | API | Rules |
|---|---|---|---|
| RegisterScreen | Register | #1 | Username and email must not be used yet (upper/lower case ignored) |
| LoginScreen | Log in | #2 | With username or email. Link to RegisterScreen |
| Whole app | Stay logged in | #3 | New access token every 15 min. After 30 days unused, log in again. A refresh token works once; using it again logs that device out |
| SettingsScreen | Account info | #5 | Shown when the screen opens, no button |
| | Change avatar | #6 | Tap the avatar. JPEG/PNG ≤ 5 MB |
| | Remove avatar | #7 | Back to the default icon |
| | Audio quality | | App setting only. Low (AAC 64k), Medium (128k), High (256k), Lossless (FLAC), or Auto (by Wi-Fi / mobile data). The player sends it in #22 |
| | Log out | #4 | Only this device |
| AdminScreen | Menu | | Create artist, Create genre, Create release. Each opens its own screen |
| CreateArtistScreen | Create artist | #9, #10 | Avatar is optional, sent after #9 |
| CreateGenreScreen | Create genre | #11 | Name must be new |
| CreateReleaseScreen | Create release | #12, #13 | 1–20 artists, 0–20 genres (from #21), cover optional, sent after #12 |
| | Upload its songs | #14 | One row per song: track number, title, FLAC. Sent after #12, max 2 at a time, each row shows waiting / uploading / done / failed (retry). FLAC 16-bit, ≤ 48 kHz, mono/stereo, ≤ 200 MB, ≤ 2 h. Track number once per release. Users see a song after encoding. No cover yet → the FLAC picture becomes it |
| ReleaseScreen | Open | #15 | Cover, type, date, artists, all songs |
| | Save / unsave | #33, #34 | |
| PlaylistScreen | Open | #17 | Private: only the owner. Others get 404 |
| | Cover | #18 | The first song's release cover |
| | Save / unsave | #33, #34 | Other users' public playlists only |
| | Edit mode | #36, #39, #40 | Owner only. Rename, make public/private, remove songs, drag to reorder |
| | Delete | #37 | Owner only. Also the owner's "remove from library" |
| ArtistScreen | Open | #19 | Name, avatar, all releases (newest first) |
| | Follow / unfollow | #33, #34 | |
| PlayerScreen | Play, seek | #22 | |
| | Favorite / unfavorite | #33, #34 | Heart button |
| | Lock screen cover | #16 | Covers are public for this |
| | Comments (scroll down) | #41 | Newest first, 10 per page, with reply count |
| | Replies | #42 | Oldest first |
| | Comment / reply | #43, #44 | Max 2000 characters. No reply to a reply |
| | Delete a comment | #45 | Author or admin. Its replies are deleted too |
| SearchScreen | Search all | #23 | Songs, releases, playlists, artists mixed. Best match first. Others' private playlists never show |
| | Search one type | #24–27 | One chip per type |
| LibraryScreen | Default list | #28 | Saved releases + playlists |
| | Filter chips | #29–32 | Releases, Playlists, Artists (followed), Songs (favorites) |
| | Search bar | #28–32 `q` | Filters by title or name |
| | Sort | #28–32 `sort` | Recent (default; newest saved first, opening also counts), Name, Artist (playlists: owner; not for artists) |
| | Create playlist (+) | #35 | Max 200 per user. Added to my library |
| SongMenu (⋮ on a song) | Favorite / unfavorite | #33, #34 | In every song list (release, playlist, search, Library Songs) |
| | Add to playlist | #30 `owned=true`, #38 | In every song list and PlayerScreen. Lists my playlists, plus "New playlist" (#35). Goes to the end. Adding it twice does nothing. Max 1000 songs |
| HomeScreen | | | Not designed yet |
