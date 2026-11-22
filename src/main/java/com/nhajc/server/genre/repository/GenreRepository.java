package com.nhajc.server.genre.repository;

import com.nhajc.server.genre.entity.Genre;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface GenreRepository extends JpaRepository<Genre, UUID> {
    @Query("select g from Genre g order by lower(g.name), g.id")
    Slice<Genre> findPage(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select g from Genre g where g.id = :id")
    Optional<Genre> findLockedById(@Param("id") UUID id);
}
