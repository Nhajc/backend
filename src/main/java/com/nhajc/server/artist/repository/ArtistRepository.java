package com.nhajc.server.artist.repository;

import com.nhajc.server.artist.entity.Artist;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface ArtistRepository extends JpaRepository<Artist, UUID> {
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select a from Artist a where a.id = :id")
    Optional<Artist> findLockedById(@Param("id") UUID id);
}
