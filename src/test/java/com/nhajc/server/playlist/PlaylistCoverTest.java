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
    final PlaylistServiceImpl service=new PlaylistServiceImpl(playlists,entries,mock(SongRepository.class),
            mock(SavedPlaylistRepository.class),users,mock(MediaViews.class),images,cleanup,new RecordingTransactions());
    Playlist owner() {
        var user=mock(User.class);when(user.getId()).thenReturn(actor);
        var playlist=new Playlist(id,"Old",true,user);playlist.setCoverObjectKey("old.png");
        when(playlists.findLockedById(id)).thenReturn(Optional.of(playlist));return playlist;
    }
    @Test void putWithoutImageClearsCoverAndQueuesOldObject() {
        var playlist=owner();
        when(images.upload(anyString(),isNull())).thenAnswer(call->{
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();return null;});
        service.update(actor,id,new PlaylistRequest("New",false),null);
        assertThat(playlist.getCoverObjectKey()).isNull();
        assertThat(playlist.getTitle()).isEqualTo("New");
        verify(cleanup).enqueue("old.png");verify(users,never()).lock(any());
    }
    @Test void putAttachesUploadedCoverInShortTransaction() {
        var playlist=owner();
        when(images.upload(anyString(),isNull())).thenAnswer(call->{
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();return "new.png";});
        doAnswer(call->{assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();return null;})
                .when(cleanup).completeUpload("new.png");
        service.update(actor,id,new PlaylistRequest("New",true),null);
        assertThat(playlist.getCoverObjectKey()).isEqualTo("new.png");
        verify(cleanup).enqueue("old.png");
    }
    @Test void removalClosesPositionGap() {
        var playlist=owner();var items=new ArrayList<PlaylistSong>();
        for(int n=0;n<3;n++){var song=mock(Song.class);when(song.getId()).thenReturn(UUID.randomUUID());items.add(new PlaylistSong(playlist,song,n));}
        when(entries.findById_PlaylistIdOrderByPositionAsc(id)).thenReturn(items);
        service.removeSong(actor,id,items.get(1).getId().getSongId());
        assertThat(items.get(0).getPosition()).isEqualTo(0);
        assertThat(items.get(2).getPosition()).isEqualTo(1);
        verify(entries).delete(items.get(1));
    }
}
