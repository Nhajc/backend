package com.nhajc.server.library.repository;

import com.nhajc.server.library.entity.FavoriteSong;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteSongRepository extends LibraryEntryRepository<FavoriteSong> {
    @Modifying
    @Query(
            value =
                    """
                    INSERT INTO user_favorite_songs (user_id, song_id, favorited_at)
                    VALUES (:user, :item, CURRENT_TIMESTAMP)
                    ON CONFLICT (user_id, song_id) DO NOTHING
                    """,
            nativeQuery = true)
    void insertIfAbsent(@Param("user") UUID user, @Param("item") UUID item);
}
