package com.nhajc.server.library.repository;

import com.nhajc.server.library.entity.SavedRelease;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SavedReleaseRepository extends LibraryEntryRepository<SavedRelease> {
    @Modifying
    @Query(
            value =
                    """
                    INSERT INTO user_saved_releases (user_id, release_id, last_activity_at)
                    VALUES (:user, :item, CURRENT_TIMESTAMP)
                    ON CONFLICT (user_id, release_id) DO NOTHING
                    """,
            nativeQuery = true)
    void insertIfAbsent(@Param("user") UUID user, @Param("item") UUID item);
}
