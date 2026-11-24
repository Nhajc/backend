package com.nhajc.server.common.service;

import com.nhajc.server.artist.dto.ArtistSummaryResponse;
import com.nhajc.server.artist.entity.Artist;
import com.nhajc.server.artist.repository.ArtistRepository;
import com.nhajc.server.release.dto.ReleaseSummaryResponse;
import com.nhajc.server.release.repository.ReleaseRepository;
import com.nhajc.server.song.dto.SongResponse;
import com.nhajc.server.song.dto.SongSummaryResponse;
import com.nhajc.server.song.repository.SongRepository;
import com.nhajc.server.playlist.dto.PlaylistSummaryResponse;
import com.nhajc.server.playlist.repository.PlaylistRepository;
import com.nhajc.server.playlist.repository.PlaylistSongRepository;
import com.nhajc.server.library.repository.*;
import com.nhajc.server.user.dto.UserSummaryResponse;
import com.nhajc.server.user.entity.User;
import com.nhajc.server.storage.service.ObjectStorageService;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class MediaViews {
    private final ArtistRepository artists;
    private final ReleaseRepository releases;
    private final SongRepository songs;
    private final PlaylistRepository playlists;
    private final PlaylistSongRepository entries;
    private final SavedReleaseRepository savedReleases;
    private final SavedPlaylistRepository savedPlaylists;
    private final FollowedArtistRepository follows;
    private final FavoriteSongRepository favorites;
    private final ObjectStorageService storage;

    public MediaViews(ArtistRepository artists, ReleaseRepository releases, SongRepository songs,
                      PlaylistRepository playlists, PlaylistSongRepository entries,
                      SavedReleaseRepository savedReleases, SavedPlaylistRepository savedPlaylists,
                      FollowedArtistRepository follows, FavoriteSongRepository favorites,
                      ObjectStorageService storage) {
        this.artists = artists;
        this.releases = releases;
        this.songs = songs;
        this.playlists = playlists;
        this.entries = entries;
        this.savedReleases = savedReleases;
        this.savedPlaylists = savedPlaylists;
        this.follows = follows;
        this.favorites = favorites;
        this.storage = storage;
    }

    public String url(String key) {
        return key == null ? null : storage.getPresignedUrl(key);
    }

    public UserSummaryResponse user(User user) {
        return new UserSummaryResponse(user.getId(), user.getUsername(), url(user.getAvatarObjectKey()));
    }

    private ArtistSummaryResponse artist(Artist artist, Map<UUID, Instant> followed) {
        return new ArtistSummaryResponse(artist.getId(), artist.getName(),
                url(artist.getAvatarObjectKey()), followed.get(artist.getId()));
    }

    public ArtistSummaryResponse artist(Artist artist, UUID actor) {
        return artist(artist, follows.savedIds(actor, List.of(artist.getId())));
    }

    private List<ArtistSummaryResponse> artistList(List<Artist> values, Map<UUID, Instant> followed) {
        return values.stream().map(value -> artist(value, followed)).toList();
    }

    public Map<UUID, ArtistSummaryResponse> artists(Collection<UUID> ids, UUID actor) {
        if (ids.isEmpty()) return Map.of();
        var followed = follows.savedIds(actor, ids);
        return artists.findAllById(ids).stream().map(value -> artist(value, followed))
                .collect(Collectors.toMap(ArtistSummaryResponse::id, Function.identity()));
    }

    public Map<UUID, ReleaseSummaryResponse> releases(Collection<UUID> ids, UUID actor) {
        if (ids.isEmpty()) return Map.of();
        var values = releases.findByIdIn(ids);
        var artistIds = values.stream().flatMap(value -> value.getArtists().stream())
                .map(Artist::getId).distinct().toList();
        var followed = follows.savedIds(actor, artistIds);
        var saved = savedReleases.savedIds(actor, ids);
        var counts = counts(songs.counts(ids));
        var result = new HashMap<UUID, ReleaseSummaryResponse>();
        for (var release : values) {
            result.put(release.getId(), new ReleaseSummaryResponse(release.getId(), release.getTitle(),
                    url(release.getCoverObjectKey()), release.getReleaseType(), release.getReleaseDate(),
                    counts.getOrDefault(release.getId(), 0L), artistList(release.getArtists(), followed),
                    saved.get(release.getId())));
        }
        return result;
    }

    public Map<UUID, SongResponse> songs(Collection<UUID> ids, UUID actor) {
        if (ids.isEmpty()) return Map.of();
        var values = songs.findByIdIn(ids);
        var artistIds = values.stream().flatMap(value -> value.getArtists().stream())
                .map(Artist::getId).distinct().toList();
        var followed = follows.savedIds(actor, artistIds);
        var saved = favorites.savedIds(actor, ids);
        var result = new HashMap<UUID, SongResponse>();
        for (var song : values) {
            var release = song.getRelease();
            result.put(song.getId(), new SongResponse(new SongSummaryResponse(song.getId(), song.getTitle(),
                    song.getDurationSeconds(), song.getTrackNumber(), artistList(song.getArtists(), followed),
                    saved.get(song.getId()), song.getDescription()), release.getId(), release.getTitle(), url(release.getCoverObjectKey())));
        }
        return result;
    }

    public Map<UUID, PlaylistSummaryResponse> playlists(Collection<UUID> ids, UUID actor) {
        if (ids.isEmpty()) return Map.of();
        var saved = savedPlaylists.savedIds(actor, ids);
        var counts = counts(entries.counts(ids));
        var result = new HashMap<UUID, PlaylistSummaryResponse>();
        for (var playlist : playlists.findByIdIn(ids)) {
            if (playlist.isPrivate() && !playlist.getOwner().getId().equals(actor)) continue;
            result.put(playlist.getId(), new PlaylistSummaryResponse(playlist.getId(), playlist.getTitle(),
                    url(playlist.getCoverObjectKey()), playlist.isPrivate(), user(playlist.getOwner()),
                    counts.getOrDefault(playlist.getId(), 0L), saved.get(playlist.getId())));
        }
        return result;
    }

    private static Map<UUID, Long> counts(List<Object[]> rows) {
        var result = new HashMap<UUID, Long>();
        for (var row : rows) result.put((UUID) row[0], ((Number) row[1]).longValue());
        return result;
    }
}
