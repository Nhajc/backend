package com.nhajc.server.artist.repository;

import com.nhajc.server.artist.entity.Artist;
import org.springframework.data.jpa.repository.*;
import java.util.UUID;

public interface ArtistRepository extends JpaRepository<Artist, UUID> {
}
