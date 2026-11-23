package com.nhajc.server.library.repository;

import com.nhajc.server.library.entity.LibraryEntry;
import com.nhajc.server.library.entity.LibraryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@NoRepositoryBean
public interface LibraryEntryRepository<T extends LibraryEntry> extends JpaRepository<T, LibraryId> {
    @Query("select e.id.itemId, e.activityAt from #{#entityName} e where e.id.userId = :user and e.id.itemId in :ids")
    List<Object[]> savedTimes(@Param("user") UUID user, @Param("ids") Collection<UUID> ids);

    default Map<UUID, Instant> savedIds(UUID user, Collection<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        var result = new HashMap<UUID, Instant>();
        for (Object[] row : savedTimes(user, ids)) {
            result.put((UUID) row[0], (Instant) row[1]);
        }
        return result;
    }

    @Modifying(flushAutomatically = true)
    @Query("""
            update #{#entityName} e
            set e.activityAt = case when e.activityAt < :time then :time else e.activityAt end
            where e.id.userId = :user and e.id.itemId = :item
            """)
    void touch(@Param("user") UUID user, @Param("item") UUID item, @Param("time") Instant time);

    @Modifying
    @Query("delete from #{#entityName} e where e.id.userId = :user and e.id.itemId = :item")
    void deleteEntry(@Param("user") UUID user, @Param("item") UUID item);
}
