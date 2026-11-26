package com.nhajc.server.comment.service.impl;

import com.nhajc.server.comment.dto.*;
import com.nhajc.server.comment.entity.Comment;
import com.nhajc.server.comment.repository.CommentRepository;
import com.nhajc.server.comment.service.CommentService;
import com.nhajc.server.common.dto.PageResponse;
import com.nhajc.server.common.service.MediaViews;
import com.nhajc.server.common.service.UserAccess;
import com.nhajc.server.song.repository.SongRepository;
import com.nhajc.server.user.repository.UserRepository;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CommentServiceImpl implements CommentService {
    private final CommentRepository comments;
    private final UserRepository users;
    private final SongRepository songs;
    private final MediaViews views;
    private final UserAccess access;

    public CommentServiceImpl(
            CommentRepository comments,
            UserRepository users,
            SongRepository songs,
            MediaViews views,
            UserAccess access) {
        this.comments = comments;
        this.users = users;
        this.songs = songs;
        this.views = views;
        this.access = access;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> list(UUID actor, UUID song, Pageable pageable) {
        var page = pageable;
        access.require(actor);
        requireSong(song);
        var slice = comments.findBySongIdAndParentIdIsNullOrderByCommentedAtDescIdDesc(song, page);
        var mapped = responses(slice.getContent());
        return PageResponse.from(slice, comment -> mapped.get(comment.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> replies(UUID actor, UUID parent, Pageable pageable) {
        var page = pageable;
        access.require(actor);
        requireTopLevel(comments.findById(parent).orElseThrow(CommentServiceImpl::missing));
        var slice = comments.findByParentIdOrderByCommentedAtAscIdAsc(parent, page);
        var mapped = responses(slice.getContent());
        return PageResponse.from(slice, comment -> mapped.get(comment.getId()));
    }

    @Override
    @Transactional
    public CommentResponse create(UUID actor, UUID song, CommentRequest request) {
        access.require(actor);
        requireSong(song);
        return store(new Comment(UUID.randomUUID(), actor, song, null, request.content()));
    }

    @Override
    @Transactional
    public CommentResponse reply(UUID actor, UUID parent, CommentRequest request) {
        access.require(actor);
        var root = comments.findLockedById(parent).orElseThrow(CommentServiceImpl::missing);
        requireTopLevel(root);
        return store(new Comment(UUID.randomUUID(), actor, root.getSongId(), parent, request.content()));
    }

    @Override
    @Transactional
    public void delete(UUID id, UUID actorId, boolean admin) {
        access.require(actorId);
        var comment = comments.findLockedById(id).orElseThrow(CommentServiceImpl::missing);
        if (!admin && !comment.getUserId().equals(actorId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot delete another user's comment.");
        comments.delete(comment);
    }

    private CommentResponse store(Comment comment) {
        comments.saveAndFlush(comment);
        return responses(List.of(comment)).get(comment.getId());
    }

    private Map<UUID, CommentResponse> responses(List<Comment> items) {
        if (items.isEmpty()) return Map.of();
        var authors =
                users.findAllById(items.stream().map(Comment::getUserId).distinct().toList()).stream()
                        .collect(Collectors.toMap(user -> user.getId(), views::user));
        var counts = new HashMap<UUID, Long>();
        for (var row : comments.replyCounts(items.stream().map(Comment::getId).toList()))
            counts.put((UUID) row[0], ((Number) row[1]).longValue());
        return items.stream()
                .collect(
                        Collectors.toMap(
                                Comment::getId,
                                comment ->
                                        new CommentResponse(
                                                comment.getId(),
                                                comment.getContent(),
                                                comment.getCommentedAt(),
                                                authors.get(comment.getUserId()),
                                                counts.getOrDefault(comment.getId(), 0L))));
    }

    private void requireSong(UUID song) {
        if (!songs.existsById(song)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Song not found.");
    }

    private static void requireTopLevel(Comment comment) {
        if (comment.getParentId() != null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Replies can only target a top-level comment.");
    }

    private static ResponseStatusException missing() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found.");
    }
}
