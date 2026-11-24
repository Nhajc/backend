package com.nhajc.server.common.service;
import com.nhajc.server.artist.entity.Artist;
import com.nhajc.server.artist.repository.ArtistRepository;
import com.nhajc.server.release.entity.*;
import com.nhajc.server.release.repository.ReleaseRepository;
import com.nhajc.server.song.entity.Song;
import com.nhajc.server.song.repository.SongRepository;
import com.nhajc.server.playlist.repository.*;
import com.nhajc.server.library.repository.*;
import com.nhajc.server.storage.service.ObjectStorageService;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class MediaViewsTest {
    @Test void nestedArtistsUseOneFollowBatchAndSongsUseNullableFavoriteTimes() {
        var artists=mock(ArtistRepository.class);var releases=mock(ReleaseRepository.class);
        var songs=mock(SongRepository.class);var follows=mock(FollowedArtistRepository.class);
        var favorites=mock(FavoriteSongRepository.class);
        var views=new MediaViews(artists,releases,songs,mock(PlaylistRepository.class),mock(PlaylistSongRepository.class),
                mock(SavedReleaseRepository.class),mock(SavedPlaylistRepository.class),follows,favorites,mock(ObjectStorageService.class));
        UUID actor=UUID.randomUUID(),first=UUID.randomUUID(),second=UUID.randomUUID();
        var artist=new Artist(UUID.randomUUID(),"Artist",null);
        var release=new Release(UUID.randomUUID(),"Album",ReleaseType.ALBUM,LocalDate.now(),null,List.of(artist),Set.of());
        var song1=new Song(first,"First",1,1,release,Map.of());
        var song2=new Song(second,"Second",1,2,release,Map.of());
        when(songs.findByIdIn(List.of(first,second))).thenReturn(List.of(song1,song2));
        Instant now=Instant.parse("2026-11-23T00:00:00Z");
        when(follows.savedIds(eq(actor),any())).thenReturn(Map.of(artist.getId(),now));
        when(favorites.savedIds(eq(actor),any())).thenReturn(Map.of(first,now));
        var result=views.songs(List.of(first,second),actor);
        assertThat(result.get(first).summary().favoritedAt()).isEqualTo(now);
        assertThat(result.get(second).summary().favoritedAt()).isNull();
        assertThat(result.get(first).summary().artists().getFirst().followedAt()).isEqualTo(now);
        assertThat(result.get(second).summary().artists().getFirst().followedAt()).isEqualTo(now);
        verify(follows,times(1)).savedIds(actor,List.of(artist.getId()));
    }
    @Test void artistsKeepCreditOrderInsteadOfNameOrder() {
        var songs=mock(SongRepository.class);var follows=mock(FollowedArtistRepository.class);
        var favorites=mock(FavoriteSongRepository.class);
        var views=new MediaViews(mock(ArtistRepository.class),mock(ReleaseRepository.class),songs,mock(PlaylistRepository.class),
                mock(PlaylistSongRepository.class),mock(SavedReleaseRepository.class),mock(SavedPlaylistRepository.class),
                follows,favorites,mock(ObjectStorageService.class));
        UUID actor=UUID.randomUUID(),id=UUID.randomUUID();
        var zed=new Artist(UUID.randomUUID(),"Zed",null);var abe=new Artist(UUID.randomUUID(),"Abe",null);
        var release=new Release(UUID.randomUUID(),"Album",ReleaseType.ALBUM,LocalDate.now(),null,List.of(zed,abe),Set.of());
        when(songs.findByIdIn(List.of(id))).thenReturn(List.of(new Song(id,"Song",1,1,release,Map.of())));
        when(follows.savedIds(eq(actor),any())).thenReturn(Map.of());
        when(favorites.savedIds(eq(actor),any())).thenReturn(Map.of());
        var artists=views.songs(List.of(id),actor).get(id).summary().artists();
        assertThat(artists).extracting(a->a.name()).containsExactly("Zed","Abe");
    }
}
