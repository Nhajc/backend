package com.nhajc.server.song.repository;

import com.nhajc.server.song.entity.Song;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface SongRepository extends JpaRepository<Song, UUID> {
    @EntityGraph(attributePaths = {"artists", "release"})
    java.util.List<Song> findByIdIn(java.util.Collection<UUID> ids);

    java.util.List<Song> findByRelease_IdOrderByTrackNumberAscIdAsc(UUID id);

    @Query("select s.release.id, count(s) from Song s where s.release.id in :ids group by s.release.id")
    java.util.List<Object[]> counts(@Param("ids") java.util.Collection<UUID> ids);
    boolean existsByRelease_IdAndTrackNumber(UUID releaseId, int trackNumber);

    @Query("""
            select a.objectKey from SongAudio a where a.id.songId = :id and a.id.quality = :quality
            and (select count(b) from SongAudio b where b.id.songId = :id) = 4
            """)
    Optional<String> findAudioKey(@Param("id") UUID id, @Param("quality") String quality);
}
