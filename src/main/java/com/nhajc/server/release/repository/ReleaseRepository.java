package com.nhajc.server.release.repository;

import com.nhajc.server.release.entity.Release;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface ReleaseRepository extends JpaRepository<Release, UUID> {
    @EntityGraph(attributePaths = "artists")
    java.util.List<Release> findByIdIn(java.util.Collection<UUID> ids);

    @Query("select r from Release r join r.artists a where a.id=:id order by r.releaseDate desc, r.id")
    org.springframework.data.domain.Slice<Release> findForArtist(@Param("id") UUID id, org.springframework.data.domain.Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Release r where r.id = :id")
    Optional<Release> findLockedById(@Param("id") UUID id);
}
