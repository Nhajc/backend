package com.nhajc.server.library;
import com.nhajc.server.artist.repository.ArtistRepository;
import com.nhajc.server.common.service.*;
import com.nhajc.server.library.entity.ItemType;
import com.nhajc.server.library.repository.*;
import com.nhajc.server.library.service.impl.LibraryServiceImpl;
import com.nhajc.server.playlist.entity.Playlist;
import com.nhajc.server.playlist.repository.PlaylistRepository;
import com.nhajc.server.release.repository.ReleaseRepository;
import com.nhajc.server.search.repository.DiscoveryRepository;
import com.nhajc.server.song.repository.SongRepository;
import com.nhajc.server.user.entity.User;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
class LibraryRemoveTest {
    @Test void unsavingAnotherUsersPrivatePlaylistDeletesTheRow() {
        var playlists=mock(PlaylistRepository.class);var saved=mock(SavedPlaylistRepository.class);
        var service=new LibraryServiceImpl(mock(UserAccess.class),mock(ArtistRepository.class),mock(ReleaseRepository.class),
                mock(SongRepository.class),playlists,mock(SavedReleaseRepository.class),saved,mock(FollowedArtistRepository.class),
                mock(FavoriteSongRepository.class),mock(DiscoveryRepository.class),mock(DiscoveryViews.class));
        UUID actor=UUID.randomUUID(),id=UUID.randomUUID();
        var owner=mock(User.class);when(owner.getId()).thenReturn(UUID.randomUUID());
        when(playlists.findById(id)).thenReturn(Optional.of(new Playlist(id,"Private",true,owner)));
        service.remove(actor,ItemType.PLAYLIST,id);
        verify(saved).deleteEntry(actor,id);
    }
}
