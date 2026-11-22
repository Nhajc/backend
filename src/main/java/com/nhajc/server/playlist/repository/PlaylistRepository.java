package com.nhajc.server.playlist.repository;
import com.nhajc.server.playlist.entity.Playlist;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface PlaylistRepository extends JpaRepository<Playlist,UUID> {
    long countByOwner_Id(UUID owner);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select p from Playlist p where p.id=:id")
    Optional<Playlist> findLockedById(@Param("id") UUID id);
    @EntityGraph(attributePaths="owner") List<Playlist> findByIdIn(Collection<UUID> ids);
}

