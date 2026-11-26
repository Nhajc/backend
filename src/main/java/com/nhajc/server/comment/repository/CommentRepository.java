package com.nhajc.server.comment.repository;

import com.nhajc.server.comment.entity.Comment;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    org.springframework.data.domain.Slice<Comment> findBySongIdAndParentIdIsNullOrderByCommentedAtDescIdDesc(
            UUID songId, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Slice<Comment> findByParentIdOrderByCommentedAtAscIdAsc(
            UUID parentId, org.springframework.data.domain.Pageable pageable);

    @Query("select c.parentId, count(c) from Comment c where c.parentId in :ids group by c.parentId")
    java.util.List<Object[]> replyCounts(@Param("ids") java.util.Collection<UUID> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Comment c where c.id = :id")
    Optional<Comment> findLockedById(@Param("id") UUID id);
}
