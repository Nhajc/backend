package com.nhajc.server.release;
import com.nhajc.server.release.service.impl.ReleaseReadServiceImpl;
import com.nhajc.server.release.repository.ReleaseRepository;
import com.nhajc.server.song.entity.Song;
import com.nhajc.server.song.repository.SongRepository;
import com.nhajc.server.song.dto.*;
import com.nhajc.server.library.repository.SavedReleaseRepository;
import com.nhajc.server.common.service.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class ReleaseFullSongsTest {
    @Test void returnsEverySongBeyondDefaultAndMaximumPageSizes() {
        var releases=mock(ReleaseRepository.class);var songs=mock(SongRepository.class);
        var views=mock(MediaViews.class);var users=mock(UserAccess.class);var saved=mock(SavedReleaseRepository.class);
        UUID actor=UUID.randomUUID(),id=UUID.randomUUID();
        when(releases.existsById(id)).thenReturn(true);
        var values=new ArrayList<Song>();var mapped=new HashMap<UUID,SongResponse>();
        for(int n=0;n<125;n++){
            UUID songId=UUID.randomUUID();var song=mock(Song.class);when(song.getId()).thenReturn(songId);values.add(song);
            mapped.put(songId,new SongResponse(new SongSummaryResponse(songId,"Song",1,n+1,List.of(),null),id,"Album",null));
        }
        when(songs.findByRelease_IdOrderByTrackNumberAscIdAsc(id)).thenReturn(values);
        when(views.songs(values.stream().map(Song::getId).toList(),actor)).thenReturn(mapped);
        when(views.releases(List.of(id),actor)).thenReturn(new HashMap<>());
        var response=new ReleaseReadServiceImpl(releases,songs,users,views,saved).get(actor,id);
        assertThat(response.songs()).hasSize(125);
        assertThat(response.songs().getLast().trackNumber()).isEqualTo(125);
        verify(users,never()).lock(actor);
    }
}
