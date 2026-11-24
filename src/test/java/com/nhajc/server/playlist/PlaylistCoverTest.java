package com.nhajc.server.playlist;
import com.nhajc.server.common.RecordingTransactions;
import com.nhajc.server.common.service.*;
import com.nhajc.server.playlist.dto.PlaylistRequest;
import com.nhajc.server.playlist.entity.*;
import com.nhajc.server.playlist.repository.*;
import com.nhajc.server.playlist.service.impl.PlaylistServiceImpl;
import com.nhajc.server.library.repository.SavedPlaylistRepository;
import com.nhajc.server.song.entity.Song;
import com.nhajc.server.song.repository.SongRepository;
import com.nhajc.server.storage.*;
import com.nhajc.server.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class PlaylistCoverTest {
    final UUID actor=UUID.randomUUID(),id=UUID.randomUUID();
    final PlaylistRepository playlists=mock(PlaylistRepository.class);
    final PlaylistSongRepository entries=mock(PlaylistSongRepository.class);
    final UserAccess users=mock(UserAccess.class);
    final ImageStorage images=mock(ImageStorage.class);
    final ObjectStorageCleanup cleanup=mock(ObjectStorageCleanup.class);
    final SavedPlaylistRepository saved=mock(SavedPlaylistRepository.class);
    final SongRepository songs=mock(SongRepository.class);
    final PlaylistServiceImpl service=new PlaylistServiceImpl(playlists,entries,songs,
            saved,users,mock(MediaViews.class),images,cleanup,new RecordingTransactions());
    Playlist owner() {
        return playlist(actor,false);
    }
    Playlist playlist(UUID ownerId,boolean isPrivate) {
        var user=mock(User.class);when(user.getId()).thenReturn(ownerId);
        var playlist=new Playlist(id,"Old",isPrivate,user);playlist.setCoverObjectKey("old.png");
        when(playlists.findById(id)).thenReturn(Optional.of(playlist));
        when(playlists.findLockedById(id)).thenReturn(Optional.of(playlist));return playlist;
    }
    @Test void putWithoutImageKeepsCover() {
        var playlist=owner();
        when(images.upload(anyString(),isNull())).thenAnswer(call->{
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();return null;});
        service.update(actor,id,new PlaylistRequest("New",false,List.of()),null);
        assertThat(playlist.getCoverObjectKey()).isEqualTo("old.png");
        assertThat(playlist.getTitle()).isEqualTo("New");
        verify(cleanup,never()).enqueue(any());verify(users,never()).lock(any());
        verify(saved,never()).deleteOthers(any(),any());
    }
    @Test void putChecksOwnerBeforeUploadingCover() {
        playlist(UUID.randomUUID(),false);
        assertThatThrownBy(()->service.update(actor,id,new PlaylistRequest("New",false,List.of()),null))
                .hasMessageContaining("Only the playlist owner");
        verifyNoInteractions(images);
    }
    @Test void makingPrivateRemovesOtherUsersSavedRows() {
        owner();
        service.update(actor,id,new PlaylistRequest("New",true,List.of()),null);
        verify(saved).deleteOthers(id,actor);
    }
    @Test void putAttachesUploadedCoverInShortTransaction() {
        var playlist=owner();
        when(images.upload(anyString(),isNull())).thenAnswer(call->{
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();return "new.png";});
        doAnswer(call->{assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();return null;})
                .when(cleanup).completeUpload("new.png");
        service.update(actor,id,new PlaylistRequest("New",true,List.of()),null);
        assertThat(playlist.getCoverObjectKey()).isEqualTo("new.png");
        verify(cleanup).enqueue("old.png");
    }
    @Test void putReplacesSongsInTheGivenOrder() {
        var playlist=owner();
        var first=mock(Song.class);when(first.getId()).thenReturn(UUID.randomUUID());
        var second=mock(Song.class);when(second.getId()).thenReturn(UUID.randomUUID());
        when(songs.findAllById(List.of(second.getId(),first.getId()))).thenReturn(List.of(first,second));
        service.update(actor,id,new PlaylistRequest("New",false,List.of(second.getId(),first.getId())),null);
        var order=inOrder(entries);
        order.verify(entries).deleteByPlaylist(id);
        order.verify(entries).save(argThat(e->e.getSong()==second&&e.getPosition()==0));
        order.verify(entries).save(argThat(e->e.getSong()==first&&e.getPosition()==1));
    }
    @Test void putRejectsRepeatedSongsBeforeUploading() {
        owner();UUID song=UUID.randomUUID();
        assertThatThrownBy(()->service.update(actor,id,new PlaylistRequest("New",false,List.of(song,song)),null))
                .hasMessageContaining("must not be repeated");
        verifyNoInteractions(images);
    }
    @Test void putRejectsUnknownSongs() {
        owner();UUID song=UUID.randomUUID();
        when(songs.findAllById(List.of(song))).thenReturn(List.of());
        assertThatThrownBy(()->service.update(actor,id,new PlaylistRequest("New",false,List.of(song)),null))
                .hasMessageContaining("Song not found");
        verify(entries,never()).deleteByPlaylist(any());
    }
}
