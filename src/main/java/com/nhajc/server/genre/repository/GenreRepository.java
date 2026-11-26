package com.nhajc.server.genre.repository;

import com.nhajc.server.genre.entity.Genre;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.*;

public interface GenreRepository extends JpaRepository<Genre, UUID> {
    @Query("select g from Genre g order by lower(g.name), g.id")
    Slice<Genre> findPage(Pageable pageable);
}
