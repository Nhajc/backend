# Nhajc use cases

- Nhajc is a music streaming app. For now, only Android.
- `#n` = endpoint number in [api.md](api.md).
- Bottom bar: Home, Search, Library, Admin (admins only).
- Top left: my avatar (#5), or the default Material 3 account icon if none → SettingsScreen.
- Image fields `avatar` and `cover` contain presigned URLs in the JSON. Load the image from that URL. A null field shows a default icon. On URL expiry, fetch the resource again for a fresh URL.

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
| | Change avatar | #6 | Tap the avatar. Send a JPEG/PNG file ≤ 5 MB in the multipart `avatar` part. Load the image from the response's `avatar` URL |
| | Remove avatar | #6 | Submit a multipart profile update with text field `_charset_=UTF-8` and the `avatar` part omitted. The response has `avatar: null`; show the default icon |
| | Audio quality | | App setting only. Low (AAC 64k), Medium (128k), High (256k), Lossless (FLAC), or Auto (by Wi-Fi / mobile data). The player sends it in #15 |
| | Log out | #4 | Only this device |
| AdminScreen | Menu | | Create artist, Create genre, Create release. Each opens its own screen |
| CreateArtistScreen | Create artist | #7 | Avatar is optional |
| CreateGenreScreen | Create genre | #8 | Name must be new |
| CreateReleaseScreen | Create release | #9 | 1–20 artists, 0–20 genres (from #14), cover optional |
| | Upload its songs | #10 | One row per song: track number, title, FLAC. Sent after #9, max 2 at a time, each row shows waiting / uploading / done / failed (retry). FLAC 16-bit, ≤ 48 kHz, mono/stereo, ≤ 200 MB, ≤ 2 h. Track number once per release. Users see a song after encoding. No cover yet → the FLAC picture becomes it |
| ReleaseScreen | Open | #11 | Cover, type, date, artists, all songs. Song rows show the track number, not the cover |
| | Save / unsave | #26, #27 | |
| PlaylistScreen | Open | #12 | Cover = the first song's release cover. Private: only the owner. Others get 404 |
| | Save / unsave | #26, #27 | Other users' public playlists only |
| | Edit mode | #29, #32, #33 | Owner only. Rename, make public/private, remove songs, drag to reorder |
| | Delete | #30 | Owner only. Also the owner's "remove from library" |
| ArtistScreen | Open | #13 | Name, avatar, all releases (newest first) |
| | Follow / unfollow | #26, #27 | |
| PlayerScreen | Play, seek | #15 | |
| | Favorite / unfavorite | #26, #27 | Heart button |
| | Lock screen cover | | The song's release cover, already in the song list |
| | Comments (scroll down) | #34 | Newest first, 10 per page. Each shows avatar, username, reply count |
| | Replies | #35 | Oldest first |
| | Comment / reply | #36, #37 | Max 2000 characters. No reply to a reply |
| | Delete a comment | #38 | Author or admin. Its replies are deleted too |
| SearchScreen | Search all | #16 | Songs, releases, playlists, artists mixed. Best match first. Others' private playlists never show |
| | Search one type | #17–20 | One chip per type |
| LibraryScreen | Default list | #21 | Saved releases + playlists |
| | Filter chips | #22–25 | Releases, Playlists, Artists (followed), Songs (favorites) |
| | Search bar | #21–25 `q` | Filters by title or name |
| | Sort | #21–25 `sort` | Recent (default; newest saved first, opening also counts), Name, Artist (playlists: owner; not for artists) |
| | Create playlist (+) | #28 | Max 200 per user. Added to my library |
| SongMenu (⋮ on a song) | Favorite / unfavorite | #26, #27 | In every song list (release, playlist, search, Library Songs) |
| | Add to playlist | #23 `owned=true`, #31 | In every song list and PlayerScreen. Lists my playlists, plus "New playlist" (#28). Goes to the end. Adding it twice does nothing. Max 1000 songs |
| HomeScreen | | | Not designed yet |
