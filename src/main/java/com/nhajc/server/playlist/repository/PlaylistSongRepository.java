package com.nhajc.server.playlist.repository;
import com.nhajc.server.playlist.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
public interface PlaylistSongRepository extends JpaRepository<PlaylistSong,PlaylistSongId> {
    long countById_PlaylistId(UUID id);
    List<PlaylistSong> findById_PlaylistIdOrderByPositionAsc(UUID id);
    @Query("select coalesce(max(p.position),-1) from PlaylistSong p where p.id.playlistId=:id")
    int lastPosition(@Param("id") UUID id);
    @Query("select p.id.playlistId, count(p) from PlaylistSong p where p.id.playlistId in :ids group by p.id.playlistId")
    List<Object[]> counts(@Param("ids") Collection<UUID> ids);
}

