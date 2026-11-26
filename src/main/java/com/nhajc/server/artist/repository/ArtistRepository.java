package com.nhajc.server.artist.repository;

import com.nhajc.server.artist.entity.Artist;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;

public interface ArtistRepository extends JpaRepository<Artist, UUID> {}
