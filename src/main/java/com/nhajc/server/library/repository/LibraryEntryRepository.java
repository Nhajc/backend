package com.nhajc.server.library.repository;
import com.nhajc.server.library.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;
import java.util.*;
@NoRepositoryBean
public interface LibraryEntryRepository<T extends LibraryEntry> extends JpaRepository<T,LibraryId> {
    @Query("select e.id.itemId from #{#entityName} e where e.id.userId=:user and e.id.itemId in :ids")
    Set<UUID> savedIds(@Param("user") UUID user, @Param("ids") Collection<UUID> ids);
}

