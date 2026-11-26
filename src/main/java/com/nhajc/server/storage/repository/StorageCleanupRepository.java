package com.nhajc.server.storage.repository;

import com.nhajc.server.storage.entity.StorageCleanupJob;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface StorageCleanupRepository extends JpaRepository<StorageCleanupJob, String> {
    @Modifying(flushAutomatically = true)
    @Query("delete from StorageCleanupJob c where c.objectKey = :key and c.claimed = false")
    int completeUpload(@Param("key") String key);

    @Modifying
    @Query("delete from StorageCleanupJob c where c.objectKey = :key and c.claimed = true")
    void deleteClaimed(@Param("key") String key);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query(
            """
            select c from StorageCleanupJob c where c.deleteAfter <= :now
              and not exists (select u.id from User u where u.avatarObjectKey = c.objectKey)
              and not exists (select a.id from Artist a where a.avatarObjectKey = c.objectKey)
              and not exists (select r.id from Release r where r.coverObjectKey = c.objectKey)
              and not exists (select p.id from Playlist p where p.coverObjectKey = c.objectKey)
              and not exists (select a.id from SongAudio a where a.objectKey = c.objectKey)
            order by c.deleteAfter, c.objectKey
            """)
    List<StorageCleanupJob> findDeletable(@Param("now") Instant now, Pageable pageable);
}
