package com.nhajc.server.playlist;
import com.nhajc.server.playlist.dto.PlaylistRequest;
import com.nhajc.server.playlist.entity.*;
import com.nhajc.server.playlist.repository.*;
import com.nhajc.server.playlist.service.impl.PlaylistServiceImpl;
import com.nhajc.server.song.entity.Song;
import com.nhajc.server.song.repository.SongRepository;
import com.nhajc.server.library.repository.SavedPlaylistRepository;
import com.nhajc.server.common.service.*;
import com.nhajc.server.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class PlaylistLimitsTest {
    final PlaylistRepository playlists=mock(PlaylistRepository.class);
    final PlaylistSongRepository entries=mock(PlaylistSongRepository.class);
    final SongRepository songs=mock(SongRepository.class);
    final UserAccess users=mock(UserAccess.class);
    final PlaylistServiceImpl service=new PlaylistServiceImpl(playlists,entries,songs,mock(SavedPlaylistRepository.class),users,mock(MediaViews.class),mock(com.nhajc.server.storage.ImageStorage.class),mock(com.nhajc.server.storage.ObjectStorageCleanup.class),manager());
    final UUID actor=UUID.randomUUID(),id=UUID.randomUUID(),songId=UUID.randomUUID();
    static org.springframework.transaction.PlatformTransactionManager manager() {
        var manager=mock(org.springframework.transaction.PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenReturn(new org.springframework.transaction.support.SimpleTransactionStatus());
        return manager;
    }
    void owner() {
        var user=mock(User.class);when(user.getId()).thenReturn(actor);
        when(playlists.findLockedById(id)).thenReturn(Optional.of(new Playlist(id,"Test",true,user)));
    }
    @Test void cannotCreateBeyondTheOwnedPlaylistLimit() {
        when(playlists.countByOwner_Id(actor)).thenReturn(200L);
        assertThatThrownBy(()->service.create(actor,new PlaylistRequest("Test",true),null))
                .isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(409));
        verify(users).lock(actor);
        verify(playlists,never()).saveAndFlush(any());
    }
    @Test void existingSongRemainsIdempotentEvenAtTheLimit() {
        owner();when(entries.existsById(new PlaylistSongId(id,songId))).thenReturn(true);
        service.addSong(actor,id,songId);
        verifyNoInteractions(songs);verify(entries,never()).save(any());
    }
    @Test void rejectsANewSongAtTheLimit() {
        owner();when(songs.findById(songId)).thenReturn(Optional.of(mock(Song.class)));
        when(entries.countById_PlaylistId(id)).thenReturn(1000L);
        assertThatThrownBy(()->service.addSong(actor,id,songId))
                .isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(409));
        verify(entries,never()).save(any());
    }
    @Test void reorderRequiresExactlyTheExistingSongSetAndRejectsDuplicates() {
        owner();var song=mock(Song.class);when(song.getId()).thenReturn(songId);
        var owner=playlists.findLockedById(id).orElseThrow();
        var entry=new PlaylistSong(owner,song,0);
        when(entries.findById_PlaylistIdOrderByPositionAsc(id)).thenReturn(List.of(entry));
        assertThatThrownBy(()->service.reorder(actor,id,List.of())).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->service.reorder(actor,id,List.of(songId,songId))).isInstanceOf(ResponseStatusException.class);
        assertThatCode(()->service.reorder(actor,id,List.of(songId))).doesNotThrowAnyException();
    }
}

