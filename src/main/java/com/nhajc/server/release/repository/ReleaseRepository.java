package com.nhajc.server.release.repository;

import com.nhajc.server.release.entity.Release;
import jakarta.persistence.LockModeType;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ReleaseRepository extends JpaRepository<Release, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Release r where r.id = :id")
    java.util.Optional<Release> findByIdForUpdate(@Param("id") UUID id);

    @EntityGraph(attributePaths = "artists")
    java.util.List<Release> findByIdIn(java.util.Collection<UUID> ids);

    @Query("select r from Release r join r.artists a where a.id=:id order by r.releaseDate desc, r.id")
    java.util.List<Release> findForArtist(@Param("id") UUID id);

    @Modifying(flushAutomatically = true)
    @Query("update Release r set r.coverObjectKey = :key where r.id = :id and r.coverObjectKey is null")
    int setCoverIfAbsent(@Param("id") UUID id, @Param("key") String key);
}
