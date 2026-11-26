package com.nhajc.server.library.repository;

import com.nhajc.server.library.entity.SavedPlaylist;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SavedPlaylistRepository extends LibraryEntryRepository<SavedPlaylist> {
    @Modifying
    @Query(
            value =
                    """
                    INSERT INTO user_saved_playlists (user_id, playlist_id, last_activity_at)
                    VALUES (:user, :item, CURRENT_TIMESTAMP)
                    ON CONFLICT (user_id, playlist_id) DO NOTHING
                    """,
            nativeQuery = true)
    void insertIfAbsent(@Param("user") UUID user, @Param("item") UUID item);

    @Modifying
    @Query("delete from SavedPlaylist e where e.id.itemId = :playlist and e.id.userId <> :owner")
    void deleteOthers(@Param("playlist") UUID playlist, @Param("owner") UUID owner);
}
