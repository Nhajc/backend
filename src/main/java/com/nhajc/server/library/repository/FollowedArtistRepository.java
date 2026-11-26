package com.nhajc.server.library.repository;

import com.nhajc.server.library.entity.FollowedArtist;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FollowedArtistRepository extends LibraryEntryRepository<FollowedArtist> {
    @Modifying
    @Query(
            value =
                    """
                    INSERT INTO user_followed_artists (user_id, artist_id, followed_at)
                    VALUES (:user, :item, CURRENT_TIMESTAMP)
                    ON CONFLICT (user_id, artist_id) DO NOTHING
                    """,
            nativeQuery = true)
    void insertIfAbsent(@Param("user") UUID user, @Param("item") UUID item);
}
